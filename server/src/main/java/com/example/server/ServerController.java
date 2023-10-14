package com.example.server;

import com.example.server.entity.Pv;
import com.example.server.entity.User;
import com.example.server.entity.Group;
import com.example.server.entity.Message;
import com.example.server.entity.Chat;
import com.example.server.repository.ChatRepository;
import com.example.server.repository.MessageRepository;
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
import java.util.List;
import java.util.Optional;

@AllArgsConstructor
@RestController
public class ServerController {
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final ChatRepository chatRepository;
    private static final int TOKEN_SIZE = 16;

    @PostMapping("/users")
    public ResponseEntity<String> newUser(@RequestBody String username) {
        if (userRepository.existsById(username)) {
            return new ResponseEntity<>(Commands.USERNAME_ALREADY_EXISTS, HttpStatus.BAD_REQUEST);
        }
        userRepository.save(new User(username, generateToken(), new ArrayList<>()));
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

    @PostMapping("/chats")
    public ResponseEntity<ChatModel> newChat(@RequestBody ChatModel chatModel) {
        if (chatModel instanceof PvModel pvModel) {
            return newPv(pvModel);
        }
        return newGroup(((GroupModel) chatModel));
    }

    private ResponseEntity<ChatModel> newPv(PvModel pvModel) {
        Optional<User> firstUser = userRepository.findById(pvModel.getFirst());
        Optional<User> secondUser = userRepository.findById(pvModel.getSecond());
        if (firstUser.isEmpty() || secondUser.isEmpty() || getPvByUsernames(pvModel.getFirst(), pvModel.getSecond()) != null) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
        Pv pv = Pv.builder().first(pvModel.getFirst()).second(pvModel.getSecond()).messages(new ArrayList<>()).build();
        chatRepository.save(pv);
        firstUser.get().getChats().add(pv);
        secondUser.get().getChats().add(pv);
        userRepository.save(firstUser.get());
        userRepository.save(secondUser.get());
        return new ResponseEntity<>(pv.toChatModel(), HttpStatus.OK);
    }

    private ResponseEntity<ChatModel> newGroup(GroupModel groupModel) {
        if (groupModel.getMembers().stream().anyMatch(username -> !userRepository.existsById(username))) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
        Group group = Group.builder()
                .owner(groupModel.getOwner())
                .members(groupModel.getMembers())
                .name(groupModel.getName())
                .messages(new ArrayList<>())
                .build();
        chatRepository.save(group);
        for (String member : group.getMembers()) {
            User user = userRepository.findById(member).get();
            user.getChats().add(group);
            userRepository.save(user);
        }
        return new ResponseEntity<>(group.toChatModel(), HttpStatus.OK);
    }

    @GetMapping("/chats/{chatId}")
    public ResponseEntity<ChatModel> getChat(@PathVariable Long chatId, @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        Optional<User> user = userRepository.findByToken(token);
        Optional<Chat> chat = chatRepository.findById(chatId);
        if (user.isEmpty() || chat.isEmpty() || !user.get().getChats().contains(chat.get())) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(chat.get().toChatModel(), HttpStatus.OK);
    }

    private Pv getPvByUsernames(String firstUsername, String secondUsername) {
        for (Chat chat : chatRepository.findAll()) {
            if (chat instanceof Pv pv &&
                    ((pv.getFirst().equals(firstUsername) && pv.getSecond().equals(secondUsername)) ||
                    (pv.getFirst().equals(secondUsername) && pv.getSecond().equals(firstUsername)))) {
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
        Optional<Chat> chat = chatRepository.findById(messageModel.getChatId());
        if (chat.isEmpty()) {
            return new ResponseEntity<>(Commands.CHAT_DOESNT_EXIST, HttpStatus.BAD_REQUEST);
        }
        if (!sender.get().getChats().contains(chat.get())) {
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
        chat.get().getMessages().add(message);
        chatRepository.save(chat.get());
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

        if (user.isEmpty() || message.isEmpty() || !user.get().getChats().contains(chatRepository.findById(message.get().getChatId()).get())) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(message.get(), HttpStatus.OK);
    }

}