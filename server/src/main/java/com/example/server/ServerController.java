package com.example.server;

import com.example.server.entity.Pv;
import com.example.server.entity.User;
import com.example.server.entity.Group;
import com.example.server.entity.Message;
import com.example.server.repository.GroupRepository;
import com.example.server.repository.MessageRepository;
import com.example.server.repository.PvRepository;
import lombok.AllArgsConstructor;
import com.example.server.repository.UserRepository;
import org.apache.commons.lang.RandomStringUtils;
import org.example.model.ChatModel;
import org.example.model.GroupModel;
import org.example.model.MessageModel;
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
    private final MessageRepository messageRepository;
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
        Pv pv = Pv.builder().first(pvModel.getFirst()).second(pvModel.getSecond()).messages(new ArrayList<>()).build();
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
                .messages(new ArrayList<>())
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

    @PostMapping("/messages")
    public ResponseEntity<String> newMessage(@RequestBody MessageModel messageModel) {
        Optional<User> sender = userRepository.findById(messageModel.getSender());
        if (sender.isEmpty()) {
            return new ResponseEntity<>(Commands.USERNAME_DOESNT_EXIST, HttpStatus.BAD_REQUEST);
        }
        Optional<Pv> pv = pvRepository.findById(messageModel.getChatId());
        Optional<Group> group = groupRepository.findById(messageModel.getChatId());
        if (pv.isEmpty() && group.isEmpty()) {
            return new ResponseEntity<>(Commands.CHAT_DOESNT_EXIST, HttpStatus.BAD_REQUEST);
        }
        if (!sender.get().getPvs().contains(pv.get()) && !sender.get().getGroups().contains(group.get())) {
            return new ResponseEntity<>(Commands.CHAT_IS_NOT_YOUR_CHAT, HttpStatus.BAD_REQUEST);
        }
        Optional<Message> repliedMessage = messageRepository.findById(messageModel.getRepliedMessageId());
        if (!messageModel.getRepliedMessageId().equals(0L) && repliedMessage.isEmpty()) {
            return new ResponseEntity<>(Commands.REPLIED_MESSAGE_DOESNT_EXIST, HttpStatus.BAD_REQUEST);
        }
        if (repliedMessage.isPresent() && !messageModel.getChatId().equals(repliedMessage.get().getChatId())) {
            return new ResponseEntity<>(Commands.REPLIED_MESSAGE_IS_NOT_IN_THIS_CHAT, HttpStatus.BAD_REQUEST);
        }
        if (messageModel.getForwardedFrom() != null && !userRepository.existsById(messageModel.getForwardedFrom())) {
            return new ResponseEntity<>(Commands.FORWARDED_FROM_USERNAME_DOESNT_EXIST, HttpStatus.BAD_REQUEST);
        }
        Message message = Message.builder()
                .text(messageModel.getText())
                .sender(messageModel.getSender())
                .chatId(messageModel.getChatId())
                .repliedMessageId(messageModel.getRepliedMessageId())
                .forwardedFrom(messageModel.getForwardedFrom())
                .build();
        messageRepository.save(message);
        if (group.isPresent()) {
            group.get().getMessages().add(message);
            groupRepository.save(group.get());
        } else {
            pv.get().getMessages().add(message);
            pvRepository.save(pv.get());
        }
        return new ResponseEntity<>(Commands.SENT, HttpStatus.OK);
    }

    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<String> deleteMessage(@PathVariable Long messageId, @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        Optional<Message> message = messageRepository.findById(messageId);
        Optional<User> user = userRepository.findByToken(token);
        if (user.isEmpty()) {
            return new ResponseEntity<>(Commands.INVALID_TOKEN, HttpStatus.BAD_REQUEST);
        }
        if (message.isEmpty()) {
            return new ResponseEntity<>(Commands.MESSAGE_DOESNT_EXIST, HttpStatus.BAD_REQUEST);
        }
        if (!message.get().getSender().equals(user.get().getUsername())) {
            return new ResponseEntity<>(Commands.MESSAGE_IS_NOT_YOUR_MESSAGE, HttpStatus.BAD_REQUEST);
        }
        messageRepository.deleteById(messageId);
        return new ResponseEntity<>(Commands.MESSAGE_DELETED, HttpStatus.OK);
    }

    @PutMapping("/messages/{messageId}")
    public ResponseEntity<String> editMessage(@PathVariable Long messageId, @RequestBody MessageModel messageModel,
                                              @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        Optional<User> user = userRepository.findByToken(token);
        if (user.isEmpty()) {
            return new ResponseEntity<>(Commands.INVALID_TOKEN, HttpStatus.BAD_REQUEST);
        }
        Optional<Message> message = messageRepository.findById(messageId);
        if (message.isEmpty()) {
            return new ResponseEntity<>(Commands.MESSAGE_DOESNT_EXIST, HttpStatus.BAD_REQUEST);
        }
        if (!message.get().getSender().equals(user.get().getUsername())) {
            return new ResponseEntity<>(Commands.MESSAGE_IS_NOT_YOUR_MESSAGE, HttpStatus.BAD_REQUEST);
        }
        message.get().setText(messageModel.getText());
        messageRepository.save(message.get());
        return new ResponseEntity<>(Commands.MESSAGE_EDITED, HttpStatus.OK);
    }

    @GetMapping("/messages/{messageId}")
    public ResponseEntity<Message> getMessage(@PathVariable Long messageId, @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        Optional<User> user = userRepository.findByToken(token);
        Optional<Message> message = messageRepository.findById(messageId);

        if (user.isEmpty() || message.isEmpty() ||
                (!user.get().getPvs().contains(pvRepository.findById(message.get().getChatId()).get())
        && !user.get().getGroups().contains(groupRepository.findById(message.get().getChatId()).get()))) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(message.get(), HttpStatus.OK);
    }

}