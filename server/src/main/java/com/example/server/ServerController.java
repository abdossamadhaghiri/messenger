package com.example.server;

import com.example.server.entity.GroupMessage;
import com.example.server.entity.Pv;
import com.example.server.entity.PvMessage;
import com.example.server.entity.User;
import com.example.server.entity.Group;
import com.example.server.entity.Message;
import com.example.server.repository.GroupMessageRepository;
import com.example.server.repository.GroupRepository;
import com.example.server.repository.PvMessageRepository;
import com.example.server.repository.PvRepository;
import lombok.AllArgsConstructor;
import com.example.server.repository.UserRepository;
import org.apache.commons.lang.RandomStringUtils;
import org.example.model.ChatModel;
import org.example.model.GroupMessageModel;
import org.example.model.GroupModel;
import org.example.model.PvMessageModel;
import org.example.model.PvModel;
import org.example.model.UserModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@AllArgsConstructor
@RestController
public class ServerController {
    private final UserRepository userRepository;
    private final PvMessageRepository pvMessageRepository;
    private final GroupMessageRepository groupMessageRepository;
    private final PvRepository pvRepository;
    private final GroupRepository groupRepository;
    private static final int TOKEN_SIZE = 16;
    private final Map<String, SseEmitter> pvEmitters = new HashMap<>();
    private final Map<String, SseEmitter> groupEmitters = new HashMap<>();

    @GetMapping(value = "pvNotifications/{username}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter getPvNotifications(@PathVariable String username) {
        SseEmitter pvEmitter = new SseEmitter(Long.MAX_VALUE);
        if (pvEmitters.containsKey(username)) {
            pvEmitters.get(username).complete();
        }
        pvEmitters.put(username, pvEmitter);
        return pvEmitter;
    }

    @GetMapping(value = "groupNotifications/{username}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter getGroupNotifications(@PathVariable String username) {
        SseEmitter groupEmitter = new SseEmitter(Long.MAX_VALUE);
        if (groupEmitters.containsKey(username)) {
            groupEmitters.get(username).complete();
        }
        groupEmitters.put(username, groupEmitter);
        return groupEmitter;
    }

    @PostMapping("/signUp")
    public ResponseEntity<String> signUp(@RequestBody String username) {
        if (userRepository.existsById(username)) {
            return new ResponseEntity<>(Commands.USERNAME_ALREADY_EXISTS, HttpStatus.CONFLICT);
        }
        userRepository.save(new User(username, generateToken(), new ArrayList<>(), new ArrayList<>()));
        return new ResponseEntity<>(Commands.SUCCESSFULLY_SIGNED_UP, HttpStatus.OK);
    }

    public static String generateToken() {
        return RandomStringUtils.randomAlphabetic(TOKEN_SIZE);
    }

    @GetMapping("signIn/{username}")
    public ResponseEntity<UserModel> signIn(@PathVariable String username) {
        if (pvEmitters.containsKey(username)) {
            return new ResponseEntity<>(null, HttpStatus.CONFLICT);
        }
        Optional<User> user = userRepository.findById(username);
        return user.map(value -> new ResponseEntity<>(value.toUserModel(), HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(null, HttpStatus.NOT_FOUND));
    }

    @GetMapping("signOut/{username}")
    public ResponseEntity<String> signOut(@PathVariable String username, @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        Optional<User> userByToken = userRepository.findByToken(token);
        if (userByToken.isEmpty()) {
            return new ResponseEntity<>(Commands.INVALID_TOKEN, HttpStatus.UNAUTHORIZED);
        }
        Optional<User> user = userRepository.findById(username);
        if (user.isEmpty()) {
            return new ResponseEntity<>(Commands.USERNAME_DOESNT_EXIST, HttpStatus.NOT_FOUND);
        }
        pvEmitters.get(username).complete();
        groupEmitters.get(username).complete();
        pvEmitters.remove(username);
        groupEmitters.remove(username);
        return new ResponseEntity<>(Commands.SUCCESSFULLY_SIGNED_OUT, HttpStatus.OK);
    }

    @GetMapping("users/{username}")
    public ResponseEntity<UserModel> getUser(@PathVariable String username, @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        Optional<User> userByToken = userRepository.findByToken(token);
        if (userByToken.isEmpty()) {
            return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
        }
        Optional<User> user = userRepository.findById(username);
        return user.map(value -> new ResponseEntity<>(value.toUserModel(), HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(null, HttpStatus.NOT_FOUND));
    }

    @PostMapping("/pvs")
    public ResponseEntity<PvModel> newPv(@RequestBody PvModel pvModel, @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        Optional<User> user = userRepository.findByToken(token);
        if (user.isEmpty()) {
            return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
        }
        Optional<User> firstUser = userRepository.findById(pvModel.getFirst());
        Optional<User> secondUser = userRepository.findById(pvModel.getSecond());
        if (firstUser.isEmpty() || secondUser.isEmpty()) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
        if (getPvByUsernames(pvModel.getFirst(), pvModel.getSecond()) != null) {
            return new ResponseEntity<>(null, HttpStatus.CONFLICT);
        }
        Pv pv = Pv.builder().first(pvModel.getFirst()).second(pvModel.getSecond()).build();
        pvRepository.save(pv);
        firstUser.get().getPvs().add(pv);
        secondUser.get().getPvs().add(pv);
        userRepository.save(firstUser.get());
        userRepository.save(secondUser.get());
        return new ResponseEntity<>(pv.toPvModel(), HttpStatus.CREATED);
    }

    @PostMapping("/groups")
    public ResponseEntity<ChatModel> newGroup(@RequestBody GroupModel groupModel,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        Optional<User> user = userRepository.findByToken(token);
        if (user.isEmpty()) {
            return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
        }
        if (groupModel.getMembers().stream().anyMatch(username -> !userRepository.existsById(username))) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
        Group group = Group.builder()
                .owner(groupModel.getOwner())
                .members(groupModel.getMembers())
                .name(groupModel.getName())
                .build();
        groupRepository.save(group);
        for (String memberUsername : group.getMembers()) {
            User member = userRepository.findById(memberUsername).get();
            member.getGroups().add(group);
            userRepository.save(member);
        }
        return new ResponseEntity<>(group.toGroupModel(), HttpStatus.CREATED);
    }

    @GetMapping("/pvs/{pvId}")
    public ResponseEntity<PvModel> getPv(@PathVariable Long pvId, @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        Optional<User> user = userRepository.findByToken(token);
        Optional<Pv> pv = pvRepository.findById(pvId);
        if (user.isEmpty() || pv.isEmpty() || !user.get().getPvs().contains(pv.get())) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(pv.get().toPvModel(), HttpStatus.OK);
    }

    @GetMapping("/groups/{groupId}")
    public ResponseEntity<GroupModel> getGroup(@PathVariable Long groupId, @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        Optional<User> user = userRepository.findByToken(token);
        Optional<Group> group = groupRepository.findById(groupId);
        if (user.isEmpty() || group.isEmpty() || !user.get().getGroups().contains(group.get())) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(group.get().toGroupModel(), HttpStatus.OK);
    }

    private Pv getPvByUsernames(String firstUsername, String secondUsername) {
        for (Pv pv : pvRepository.findAll()) {
            if ((pv.getFirst().equals(firstUsername) && pv.getSecond().equals(secondUsername)) ||
                    (pv.getFirst().equals(secondUsername) && pv.getSecond().equals(firstUsername))) {
                return pv;
            }
        }
        return null;
    }

    @PostMapping("/pvMessages")
    public ResponseEntity<String> newPvMessage(@RequestBody PvMessageModel pvMessageModel,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        Optional<User> user = userRepository.findByToken(token);
        if (user.isEmpty()) {
            return new ResponseEntity<>(Commands.INVALID_TOKEN, HttpStatus.UNAUTHORIZED);
        }
        Optional<User> sender = userRepository.findById(pvMessageModel.getSender());
        if (sender.isEmpty()) {
            return new ResponseEntity<>(Commands.USERNAME_DOESNT_EXIST, HttpStatus.NOT_FOUND);
        }
        Optional<Pv> pv = pvRepository.findById(pvMessageModel.getPvId());
        if (pv.isEmpty()) {
            return new ResponseEntity<>(Commands.CHAT_DOESNT_EXIST, HttpStatus.NOT_FOUND);
        }
        if (!sender.get().getPvs().contains(pv.get())) {
            return new ResponseEntity<>(Commands.CHAT_IS_NOT_YOUR_CHAT, HttpStatus.FORBIDDEN);
        }
        Optional<PvMessage> repliedMessage = pvMessageRepository.findById(pvMessageModel.getRepliedMessageId());
        if (!pvMessageModel.getRepliedMessageId().equals(0L) && repliedMessage.isEmpty()) {
            return new ResponseEntity<>(Commands.REPLIED_MESSAGE_DOESNT_EXIST, HttpStatus.NOT_FOUND);
        }
        if (repliedMessage.isPresent() && !pvMessageModel.getPvId().equals(repliedMessage.get().getPvId())) {
            return new ResponseEntity<>(Commands.REPLIED_MESSAGE_IS_NOT_IN_THIS_CHAT, HttpStatus.FORBIDDEN);
        }
        if (pvMessageModel.getForwardedFrom() != null && !userRepository.existsById(pvMessageModel.getForwardedFrom())) {
            return new ResponseEntity<>(Commands.FORWARDED_FROM_USERNAME_DOESNT_EXIST, HttpStatus.NOT_FOUND);
        }
        PvMessage pvMessage = PvMessage.builder()
                .text(pvMessageModel.getText())
                .sender(pvMessageModel.getSender())
                .pvId(pvMessageModel.getPvId())
                .repliedMessageId(pvMessageModel.getRepliedMessageId())
                .forwardedFrom(pvMessageModel.getForwardedFrom())
                .build();
        pvMessageRepository.save(pvMessage);
        pv.get().getPvMessages().add(pvMessage);
        pvRepository.save(pv.get());
        String peerUsername = pv.get().getFirst().equals(sender.get().getUsername()) ? pv.get().getSecond() : pv.get().getFirst();
        if (pvEmitters.containsKey(peerUsername)) {
            try {
                pvEmitters.get(peerUsername).send(pvMessage.toPvMessageModel());
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
        return new ResponseEntity<>(Commands.SENT, HttpStatus.OK);
    }

    @PostMapping("/groupMessages")
    public ResponseEntity<String> newGroupMessage(@RequestBody GroupMessageModel groupMessageModel,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        Optional<User> user = userRepository.findByToken(token);
        if (user.isEmpty()) {
            return new ResponseEntity<>(Commands.INVALID_TOKEN, HttpStatus.UNAUTHORIZED);
        }
        Optional<User> sender = userRepository.findById(groupMessageModel.getSender());
        if (sender.isEmpty()) {
            return new ResponseEntity<>(Commands.USERNAME_DOESNT_EXIST, HttpStatus.NOT_FOUND);
        }
        Optional<Group> group = groupRepository.findById(groupMessageModel.getGroupId());
        if (group.isEmpty()) {
            return new ResponseEntity<>(Commands.CHAT_DOESNT_EXIST, HttpStatus.NOT_FOUND);
        }
        if (!sender.get().getGroups().contains(group.get())) {
            return new ResponseEntity<>(Commands.CHAT_IS_NOT_YOUR_CHAT, HttpStatus.FORBIDDEN);
        }
        Optional<GroupMessage> repliedMessage = groupMessageRepository.findById(groupMessageModel.getRepliedMessageId());
        if (!groupMessageModel.getRepliedMessageId().equals(0L) && repliedMessage.isEmpty()) {
            return new ResponseEntity<>(Commands.REPLIED_MESSAGE_DOESNT_EXIST, HttpStatus.NOT_FOUND);
        }
        if (repliedMessage.isPresent() && !groupMessageModel.getGroupId().equals(repliedMessage.get().getGroupId())) {
            return new ResponseEntity<>(Commands.REPLIED_MESSAGE_IS_NOT_IN_THIS_CHAT, HttpStatus.FORBIDDEN);
        }
        if (groupMessageModel.getForwardedFrom() != null && !userRepository.existsById(groupMessageModel.getForwardedFrom())) {
            return new ResponseEntity<>(Commands.FORWARDED_FROM_USERNAME_DOESNT_EXIST, HttpStatus.NOT_FOUND);
        }
        GroupMessage groupMessage = GroupMessage.builder()
                .text(groupMessageModel.getText())
                .sender(groupMessageModel.getSender())
                .groupId(groupMessageModel.getGroupId())
                .repliedMessageId(groupMessageModel.getRepliedMessageId())
                .forwardedFrom(groupMessageModel.getForwardedFrom())
                .build();
        groupMessageRepository.save(groupMessage);
        group.get().getGroupMessages().add(groupMessage);
        groupRepository.save(group.get());
        List<String> others = group.get().getMembers();
        others.remove(groupMessage.getSender());
        for (String peerUsername : others) {
            if (groupEmitters.containsKey(peerUsername)) {
                try {
                    groupEmitters.get(peerUsername).send(groupMessage.toGroupMessageModel());
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }
            }
        }
        return new ResponseEntity<>(Commands.SENT, HttpStatus.OK);
    }

    @DeleteMapping("/pvMessages/{pvMessageId}")
    public ResponseEntity<String> deletePvMessage(@PathVariable Long pvMessageId, @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        Optional<PvMessage> pvMessage = pvMessageRepository.findById(pvMessageId);
        Optional<User> user = userRepository.findByToken(token);
        if (user.isEmpty()) {
            return new ResponseEntity<>(Commands.INVALID_TOKEN, HttpStatus.UNAUTHORIZED);
        }
        if (pvMessage.isEmpty()) {
            return new ResponseEntity<>(Commands.MESSAGE_DOESNT_EXIST, HttpStatus.NOT_FOUND);
        }
        if (!pvMessage.get().getSender().equals(user.get().getUsername())) {
            return new ResponseEntity<>(Commands.MESSAGE_IS_NOT_YOUR_MESSAGE, HttpStatus.FORBIDDEN);
        }
        pvMessageRepository.deleteById(pvMessageId);
        return new ResponseEntity<>(Commands.MESSAGE_DELETED, HttpStatus.OK);
    }

    @DeleteMapping("/groupMessages/{groupMessageId}")
    public ResponseEntity<String> deleteGroupMessage(@PathVariable Long groupMessageId, @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        Optional<GroupMessage> groupMessage = groupMessageRepository.findById(groupMessageId);
        Optional<User> user = userRepository.findByToken(token);
        if (user.isEmpty()) {
            return new ResponseEntity<>(Commands.INVALID_TOKEN, HttpStatus.UNAUTHORIZED);
        }
        if (groupMessage.isEmpty()) {
            return new ResponseEntity<>(Commands.MESSAGE_DOESNT_EXIST, HttpStatus.NOT_FOUND);
        }
        if (!groupMessage.get().getSender().equals(user.get().getUsername())) {
            return new ResponseEntity<>(Commands.MESSAGE_IS_NOT_YOUR_MESSAGE, HttpStatus.FORBIDDEN);
        }
        groupMessageRepository.deleteById(groupMessageId);
        return new ResponseEntity<>(Commands.MESSAGE_DELETED, HttpStatus.OK);
    }

    @PutMapping("/pvMessages/{pvMessageId}")
    public ResponseEntity<String> editPvMessage(@PathVariable Long pvMessageId, @RequestBody PvMessageModel pvMessageModel,
                                              @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        Optional<User> user = userRepository.findByToken(token);
        if (user.isEmpty()) {
            return new ResponseEntity<>(Commands.INVALID_TOKEN, HttpStatus.UNAUTHORIZED);
        }
        Optional<PvMessage> pvMessage = pvMessageRepository.findById(pvMessageId);
        if (pvMessage.isEmpty()) {
            return new ResponseEntity<>(Commands.MESSAGE_DOESNT_EXIST, HttpStatus.NOT_FOUND);
        }
        if (!pvMessage.get().getSender().equals(user.get().getUsername())) {
            return new ResponseEntity<>(Commands.MESSAGE_IS_NOT_YOUR_MESSAGE, HttpStatus.FORBIDDEN);
        }
        pvMessage.get().setText(pvMessageModel.getText());
        pvMessageRepository.save(pvMessage.get());
        return new ResponseEntity<>(Commands.MESSAGE_EDITED, HttpStatus.OK);
    }

    @PutMapping("/groupMessages/{groupMessageId}")
    public ResponseEntity<String> editGroupMessage(@PathVariable Long groupMessageId, @RequestBody GroupMessageModel groupMessageModel,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        Optional<User> user = userRepository.findByToken(token);
        if (user.isEmpty()) {
            return new ResponseEntity<>(Commands.INVALID_TOKEN, HttpStatus.UNAUTHORIZED);
        }
        Optional<GroupMessage> groupMessage = groupMessageRepository.findById(groupMessageId);
        if (groupMessage.isEmpty()) {
            return new ResponseEntity<>(Commands.MESSAGE_DOESNT_EXIST, HttpStatus.NOT_FOUND);
        }
        if (!groupMessage.get().getSender().equals(user.get().getUsername())) {
            return new ResponseEntity<>(Commands.MESSAGE_IS_NOT_YOUR_MESSAGE, HttpStatus.FORBIDDEN);
        }
        groupMessage.get().setText(groupMessageModel.getText());
        groupMessageRepository.save(groupMessage.get());
        return new ResponseEntity<>(Commands.MESSAGE_EDITED, HttpStatus.OK);
    }

    @GetMapping("/pvMessages/{pvMessageId}")
    public ResponseEntity<Message> getPvMessage(@PathVariable Long pvMessageId, @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        Optional<User> user = userRepository.findByToken(token);
        Optional<PvMessage> pvMessage = pvMessageRepository.findById(pvMessageId);
        if (user.isEmpty()) {
            return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
        }
        if (pvMessage.isEmpty()) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
        Pv pv = pvRepository.findById(pvMessage.get().getPvId()).get();
        if (!user.get().getPvs().contains(pv)) {
            return new ResponseEntity<>(null, HttpStatus.FORBIDDEN);
        }
        return new ResponseEntity<>(pvMessage.get(), HttpStatus.OK);
    }

    @GetMapping("/groupMessages/{groupMessageId}")
    public ResponseEntity<Message> getGroupMessage(@PathVariable Long groupMessageId, @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        Optional<User> user = userRepository.findByToken(token);
        Optional<GroupMessage> groupMessage = groupMessageRepository.findById(groupMessageId);
        if (user.isEmpty()) {
            return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
        }
        if (groupMessage.isEmpty()) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
        Group group = groupRepository.findById(groupMessage.get().getGroupId()).get();
        if (!user.get().getGroups().contains(group)) {
            return new ResponseEntity<>(null, HttpStatus.FORBIDDEN);
        }
        return new ResponseEntity<>(groupMessage.get(), HttpStatus.OK);
    }

}