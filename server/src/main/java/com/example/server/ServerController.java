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

    @PostMapping("/signUp")
    public ResponseEntity<String> signUp(@RequestBody String username) {
        if (userRepository.existsById(username)) {
            return new ResponseEntity<>(Commands.USERNAME_ALREADY_EXISTS, HttpStatus.BAD_REQUEST);
        }
        userRepository.save(new User(username, generateToken()));
        return new ResponseEntity<>(Commands.SUCCESSFULLY_SIGNED_UP, HttpStatus.OK);
    }

    public static String generateToken() {
        return RandomStringUtils.randomAlphabetic(TOKEN_SIZE);
    }

    @GetMapping("signIn/{username}")
    public ResponseEntity<UserModel> signIn(@PathVariable String username) {
        Optional<User> user = userRepository.findById(username);
        return user.map(value -> new ResponseEntity<>(value.createUserModel(), HttpStatus.OK)).orElseGet(() -> new ResponseEntity<>(null, HttpStatus.BAD_REQUEST));
    }

    @GetMapping("/chats/{username}")
    public ResponseEntity<List<ChatModel>> getChats(@PathVariable String username) {
        if (!userRepository.existsById(username)) {
            return new ResponseEntity<>(new ArrayList<>(), HttpStatus.BAD_REQUEST);
        }
        List<ChatModel> chatModels = new ArrayList<>();
        for (Chat chat : chatRepository.findAll()) {
            if (isInChats(username, chat)) {
                chatModels.add(chat.createChatModel());
            }
        }
        return new ResponseEntity<>(chatModels, HttpStatus.OK);
    }

    @GetMapping("/messages/{username}/{chatId}")
    public ResponseEntity<List<MessageModel>> getMessagesInOldChat(@PathVariable String username, @PathVariable Long chatId) {
        if (!userRepository.existsById(username) || chatRepository.findById(chatId).isEmpty() || !isInChats(username, chatRepository.findById(chatId).get())) {
            return new ResponseEntity<>(new ArrayList<>(), HttpStatus.BAD_REQUEST);
        }
        List<MessageModel> messageModels = new ArrayList<>();
        messageRepository.findMessagesByChatId(chatId).forEach(message -> messageModels.add(message.createMessageModel()));
        return new ResponseEntity<>(messageModels, HttpStatus.OK);
    }

    @PostMapping("/chat")
    public ResponseEntity<String> newPv(@RequestBody PvModel pvModel) {
        if (!userRepository.existsById(pvModel.getFirst()) || !userRepository.existsById(pvModel.getSecond())) {
            return new ResponseEntity<>(Commands.USERNAME_DOESNT_EXIST, HttpStatus.BAD_REQUEST);
        } else if (getPvByUsernames(pvModel.getFirst(), pvModel.getSecond()) != null) {
            return new ResponseEntity<>(Commands.CHAT_ALREADY_EXISTS, HttpStatus.BAD_REQUEST);
        } else {
            Pv pv = new Pv(pvModel.getFirst(), pvModel.getSecond());
            chatRepository.save(pv);
            return new ResponseEntity<>(Commands.START_YOUR_CHAT, HttpStatus.OK);
        }
    }

    private boolean isInChats(String myUsername, Chat chat) {
        if (chat instanceof Pv pv) {
            return pv.getFirst().equals(myUsername) || pv.getSecond().equals(myUsername);
        }
        return ((Group) chat).getMembers().contains(myUsername);
    }

    private Pv getPvByUsernames(String firstUsername, String secondUsername) {
        for (Chat chat : chatRepository.findAll()) {
            if (chat instanceof Pv pv && ((pv.getFirst().equals(firstUsername) && pv.getSecond().equals(secondUsername)) ||
                    (pv.getFirst().equals(secondUsername) && pv.getSecond().equals(firstUsername)))) {
                return pv;

            }
        }
        return null;
    }

    @GetMapping("/chatId/{firstUsername}/{secondUsername}")
    public ResponseEntity<Long> getChatId(@PathVariable String firstUsername, @PathVariable String secondUsername) {
        Pv pv = getPvByUsernames(firstUsername, secondUsername);
        if (userRepository.existsById(firstUsername) && userRepository.existsById(secondUsername) && pv != null) {
            return new ResponseEntity<>(pv.getId(), HttpStatus.OK);
        }
        return new ResponseEntity<>(-1L, HttpStatus.BAD_REQUEST);
    }

    @PostMapping("/messages")
    public ResponseEntity<String> newMessage(@RequestBody MessageModel messageModel) {
        if (!userRepository.existsById(messageModel.getSender())) {
            return new ResponseEntity<>(Commands.USERNAME_DOESNT_EXIST, HttpStatus.BAD_REQUEST);
        }
        Optional<Chat> chat = chatRepository.findById(messageModel.getChatId());
        if (chat.isEmpty()) {
            return new ResponseEntity<>(Commands.CHAT_DOESNT_EXIST, HttpStatus.BAD_REQUEST);
        }
        if (!isInChats(messageModel.getSender(), chat.get())) {
            return new ResponseEntity<>(Commands.CHAT_IS_NOT_YOUR_CHAT, HttpStatus.BAD_REQUEST);
        }
        Optional<Message> repliedMessage = messageRepository.findById(messageModel.getRepliedMessageId());
        if (!messageModel.getRepliedMessageId().equals(0L) && repliedMessage.isEmpty()) {
            return new ResponseEntity<>(Commands.REPLIED_MESSAGE_DOESNT_EXIST, HttpStatus.BAD_REQUEST);
        }
        if (repliedMessage.isPresent() && !messageModel.getChatId().equals(repliedMessage.get().getChatId())) {
            return new ResponseEntity<>(Commands.REPLIED_MESSAGE_IS_NOT_IN_THIS_CHAT, HttpStatus.BAD_REQUEST);
        }
        Message message = new Message(messageModel.getText(), messageModel.getSender(), messageModel.getChatId(), messageModel.getRepliedMessageId());
        messageRepository.save(message);
        return new ResponseEntity<>(Commands.SENT, HttpStatus.OK);
    }

    @PostMapping("/groups")
    public ResponseEntity<String> newGroup(@RequestBody GroupModel groupModel) {
        if (groupModel.getMembers().stream().anyMatch(username -> !userRepository.existsById(username))) {
            return new ResponseEntity<>(Commands.INVALID_GROUP_CONTENT, HttpStatus.BAD_REQUEST);
        }
        Group group = new Group(groupModel.getOwner(), groupModel.getMembers(), groupModel.getName());
        chatRepository.save(group);
        return new ResponseEntity<>(Commands.GROUP_SUCCESSFULLY_CREATED, HttpStatus.OK);
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
        if (user.isEmpty() || message.isEmpty() || !message.get().getSender().equals(user.get().getUsername())) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(message.get(), HttpStatus.OK);
    }

}