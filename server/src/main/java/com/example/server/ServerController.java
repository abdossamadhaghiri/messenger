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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.util.ArrayList;
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

    @PostMapping("/users")
    public ResponseEntity<String> newUser(@RequestBody String username) {
        if (userRepository.existsById(username)) {
            return new ResponseEntity<>(Commands.USERNAME_ALREADY_EXISTS, HttpStatus.BAD_REQUEST);
        }
        userRepository.save(new User(username, generateToken(), new ArrayList<>(), new ArrayList<>()));
        return new ResponseEntity<>(Commands.SUCCESSFULLY_SIGNED_UP, HttpStatus.OK);
    }

    public static String generateToken() {
        return RandomStringUtils.randomAlphabetic(TOKEN_SIZE);
    }

    @GetMapping("users/{username}")
    public ResponseEntity<UserModel> getUser(@PathVariable String username) {
        Optional<User> user = userRepository.findById(username);
        return user.map(value -> new ResponseEntity<>(value.toUserModel(), HttpStatus.OK)).orElseGet(() -> new ResponseEntity<>(null, HttpStatus.BAD_REQUEST));
    }

    @PostMapping("/pvs")
    public ResponseEntity<PvModel> newPv(@RequestBody PvModel pvModel) {
        Optional<User> firstUser = userRepository.findById(pvModel.getFirst());
        Optional<User> secondUser = userRepository.findById(pvModel.getSecond());
        if (firstUser.isEmpty() || secondUser.isEmpty() || getPvByUsernames(pvModel.getFirst(), pvModel.getSecond()) != null) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
        Pv pv = Pv.builder().first(pvModel.getFirst()).second(pvModel.getSecond()).build();
        pvRepository.save(pv);
        firstUser.get().getPvs().add(pv);
        secondUser.get().getPvs().add(pv);
        userRepository.save(firstUser.get());
        userRepository.save(secondUser.get());
        return new ResponseEntity<>(pv.toPvModel(), HttpStatus.OK);
    }

    @PostMapping("/groups")
    public ResponseEntity<ChatModel> newGroup(@RequestBody GroupModel groupModel) {
        if (groupModel.getMembers().stream().anyMatch(username -> !userRepository.existsById(username))) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
        Group group = Group.builder()
                .owner(groupModel.getOwner())
                .members(groupModel.getMembers())
                .name(groupModel.getName())
                .build();
        groupRepository.save(group);
        for (String member : group.getMembers()) {
            User user = userRepository.findById(member).get();
            user.getGroups().add(group);
            userRepository.save(user);
        }
        return new ResponseEntity<>(group.toGroupModel(), HttpStatus.OK);
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
    public ResponseEntity<String> newPvMessage(@RequestBody PvMessageModel pvMessageModel) {
        Optional<User> sender = userRepository.findById(pvMessageModel.getSender());
        if (sender.isEmpty()) {
            return new ResponseEntity<>(Commands.USERNAME_DOESNT_EXIST, HttpStatus.BAD_REQUEST);
        }
        Optional<Pv> pv = pvRepository.findById(pvMessageModel.getPvId());
        if (pv.isEmpty()) {
            return new ResponseEntity<>(Commands.CHAT_DOESNT_EXIST, HttpStatus.BAD_REQUEST);
        }
        if (!sender.get().getPvs().contains(pv.get())) {
            return new ResponseEntity<>(Commands.CHAT_IS_NOT_YOUR_CHAT, HttpStatus.BAD_REQUEST);
        }
        Optional<PvMessage> repliedMessage = pvMessageRepository.findById(pvMessageModel.getRepliedMessageId());
        if (!pvMessageModel.getRepliedMessageId().equals(0L) && repliedMessage.isEmpty()) {
            return new ResponseEntity<>(Commands.REPLIED_MESSAGE_DOESNT_EXIST, HttpStatus.BAD_REQUEST);
        }
        if (repliedMessage.isPresent() && !pvMessageModel.getPvId().equals(repliedMessage.get().getPvId())) {
            return new ResponseEntity<>(Commands.REPLIED_MESSAGE_IS_NOT_IN_THIS_CHAT, HttpStatus.BAD_REQUEST);
        }
        if (pvMessageModel.getForwardedFrom() != null && !userRepository.existsById(pvMessageModel.getForwardedFrom())) {
            return new ResponseEntity<>(Commands.FORWARDED_FROM_USERNAME_DOESNT_EXIST, HttpStatus.BAD_REQUEST);
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
        return new ResponseEntity<>(Commands.SENT, HttpStatus.OK);
    }

    @PostMapping("/groupMessages")
    public ResponseEntity<String> newGroupMessage(@RequestBody GroupMessageModel groupMessageModel) {
        Optional<User> sender = userRepository.findById(groupMessageModel.getSender());
        if (sender.isEmpty()) {
            return new ResponseEntity<>(Commands.USERNAME_DOESNT_EXIST, HttpStatus.BAD_REQUEST);
        }
        Optional<Group> group = groupRepository.findById(groupMessageModel.getGroupId());
        if (group.isEmpty()) {
            return new ResponseEntity<>(Commands.CHAT_DOESNT_EXIST, HttpStatus.BAD_REQUEST);
        }
        if (!sender.get().getGroups().contains(group.get())) {
            return new ResponseEntity<>(Commands.CHAT_IS_NOT_YOUR_CHAT, HttpStatus.BAD_REQUEST);
        }
        Optional<GroupMessage> repliedMessage = groupMessageRepository.findById(groupMessageModel.getRepliedMessageId());
        if (!groupMessageModel.getRepliedMessageId().equals(0L) && repliedMessage.isEmpty()) {
            return new ResponseEntity<>(Commands.REPLIED_MESSAGE_DOESNT_EXIST, HttpStatus.BAD_REQUEST);
        }
        if (repliedMessage.isPresent() && !groupMessageModel.getGroupId().equals(repliedMessage.get().getGroupId())) {
            return new ResponseEntity<>(Commands.REPLIED_MESSAGE_IS_NOT_IN_THIS_CHAT, HttpStatus.BAD_REQUEST);
        }
        if (groupMessageModel.getForwardedFrom() != null && !userRepository.existsById(groupMessageModel.getForwardedFrom())) {
            return new ResponseEntity<>(Commands.FORWARDED_FROM_USERNAME_DOESNT_EXIST, HttpStatus.BAD_REQUEST);
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
        return new ResponseEntity<>(Commands.SENT, HttpStatus.OK);
    }

    @DeleteMapping("/pvMessages/{pvMessageId}")
    public ResponseEntity<String> deletePvMessage(@PathVariable Long pvMessageId, @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        Optional<PvMessage> pvMessage = pvMessageRepository.findById(pvMessageId);
        Optional<User> user = userRepository.findByToken(token);
        if (user.isEmpty()) {
            return new ResponseEntity<>(Commands.INVALID_TOKEN, HttpStatus.BAD_REQUEST);
        }
        if (pvMessage.isEmpty()) {
            return new ResponseEntity<>(Commands.MESSAGE_DOESNT_EXIST, HttpStatus.BAD_REQUEST);
        }
        if (!pvMessage.get().getSender().equals(user.get().getUsername())) {
            return new ResponseEntity<>(Commands.MESSAGE_IS_NOT_YOUR_MESSAGE, HttpStatus.BAD_REQUEST);
        }
        pvMessageRepository.deleteById(pvMessageId);
        return new ResponseEntity<>(Commands.MESSAGE_DELETED, HttpStatus.OK);
    }

    @DeleteMapping("/groupMessages/{groupMessageId}")
    public ResponseEntity<String> deleteGroupMessage(@PathVariable Long groupMessageId, @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        Optional<GroupMessage> groupMessage = groupMessageRepository.findById(groupMessageId);
        Optional<User> user = userRepository.findByToken(token);
        if (user.isEmpty()) {
            return new ResponseEntity<>(Commands.INVALID_TOKEN, HttpStatus.BAD_REQUEST);
        }
        if (groupMessage.isEmpty()) {
            return new ResponseEntity<>(Commands.MESSAGE_DOESNT_EXIST, HttpStatus.BAD_REQUEST);
        }
        if (!groupMessage.get().getSender().equals(user.get().getUsername())) {
            return new ResponseEntity<>(Commands.MESSAGE_IS_NOT_YOUR_MESSAGE, HttpStatus.BAD_REQUEST);
        }
        groupMessageRepository.deleteById(groupMessageId);
        return new ResponseEntity<>(Commands.MESSAGE_DELETED, HttpStatus.OK);
    }

    @PutMapping("/pvMessages/{pvMessageId}")
    public ResponseEntity<String> editPvMessage(@PathVariable Long pvMessageId, @RequestBody PvMessageModel pvMessageModel,
                                              @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        Optional<User> user = userRepository.findByToken(token);
        if (user.isEmpty()) {
            return new ResponseEntity<>(Commands.INVALID_TOKEN, HttpStatus.BAD_REQUEST);
        }
        Optional<PvMessage> pvMessage = pvMessageRepository.findById(pvMessageId);
        if (pvMessage.isEmpty()) {
            return new ResponseEntity<>(Commands.MESSAGE_DOESNT_EXIST, HttpStatus.BAD_REQUEST);
        }
        if (!pvMessage.get().getSender().equals(user.get().getUsername())) {
            return new ResponseEntity<>(Commands.MESSAGE_IS_NOT_YOUR_MESSAGE, HttpStatus.BAD_REQUEST);
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
            return new ResponseEntity<>(Commands.INVALID_TOKEN, HttpStatus.BAD_REQUEST);
        }
        Optional<GroupMessage> groupMessage = groupMessageRepository.findById(groupMessageId);
        if (groupMessage.isEmpty()) {
            return new ResponseEntity<>(Commands.MESSAGE_DOESNT_EXIST, HttpStatus.BAD_REQUEST);
        }
        if (!groupMessage.get().getSender().equals(user.get().getUsername())) {
            return new ResponseEntity<>(Commands.MESSAGE_IS_NOT_YOUR_MESSAGE, HttpStatus.BAD_REQUEST);
        }
        groupMessage.get().setText(groupMessageModel.getText());
        groupMessageRepository.save(groupMessage.get());
        return new ResponseEntity<>(Commands.MESSAGE_EDITED, HttpStatus.OK);
    }

    @GetMapping("/pvMessages/{pvMessageId}")
    public ResponseEntity<Message> getPvMessage(@PathVariable Long pvMessageId, @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        Optional<User> user = userRepository.findByToken(token);
        Optional<PvMessage> pvMessage = pvMessageRepository.findById(pvMessageId);
        if (user.isEmpty() || pvMessage.isEmpty() ||
                !user.get().getPvs().contains(pvRepository.findById(pvMessage.get().getPvId()).get())) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(pvMessage.get(), HttpStatus.OK);
    }

    @GetMapping("/groupMessages/{groupMessageId}")
    public ResponseEntity<Message> getGroupMessage(@PathVariable Long groupMessageId, @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        Optional<User> user = userRepository.findByToken(token);
        Optional<GroupMessage> groupMessage = groupMessageRepository.findById(groupMessageId);
        if (user.isEmpty() || groupMessage.isEmpty() ||
                !user.get().getPvs().contains(pvRepository.findById(groupMessage.get().getGroupId()).get())) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(groupMessage.get(), HttpStatus.OK);
    }

}