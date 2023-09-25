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
import org.example.model.ChatModel;
import org.example.model.GroupModel;
import org.example.model.MessageModel;
import org.example.model.PvModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @PostMapping("/signUp")
    public ResponseEntity<String> signUp(@RequestBody String username) {
        if (userRepository.existsById(username)) {
            return new ResponseEntity<>("this username already exists", HttpStatus.BAD_REQUEST);
        }
        userRepository.save(new User(username));
        return new ResponseEntity<>("successfully signed up", HttpStatus.OK);
    }

    @GetMapping("signIn/{username}")
    public ResponseEntity<String> signIn(@PathVariable String username) {
        if (userRepository.existsById(username)) {
            return new ResponseEntity<>("successfully signed in", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("this username doesnt exist", HttpStatus.BAD_REQUEST);
        }
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
            return new ResponseEntity<>("username doesnt exist.", HttpStatus.BAD_REQUEST);
        } else if (getPvByUsernames(pvModel.getFirst(), pvModel.getSecond()) != null) {
            return new ResponseEntity<>("the chat already exists.", HttpStatus.BAD_REQUEST);
        } else {
            Pv pv = new Pv(pvModel.getFirst(), pvModel.getSecond());
            chatRepository.save(pv);
            return new ResponseEntity<>("start your chat.", HttpStatus.OK);
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
        if (!userRepository.existsById(messageModel.getSender()) || !chatRepository.existsById(messageModel.getChatId())) {
            return new ResponseEntity<>("invalid message content!", HttpStatus.BAD_REQUEST);
        }
        Message message = new Message(messageModel.getText(), messageModel.getSender(), messageModel.getChatId());
        messageRepository.save(message);
        return new ResponseEntity<>("sent!", HttpStatus.OK);
    }

    @PostMapping("/groups")
    public ResponseEntity<String> newGroup(@RequestBody GroupModel groupModel) {
        if (groupModel.getMembers().stream().anyMatch(username -> !userRepository.existsById(username))) {
            return new ResponseEntity<>("invalid group content!", HttpStatus.BAD_REQUEST);
        }
        Group group = new Group(groupModel.getOwner(), groupModel.getMembers(), groupModel.getName());
        chatRepository.save(group);
        return new ResponseEntity<>("the group successfully created!", HttpStatus.OK);
    }

    @DeleteMapping("/messages/{messageId}/{chatId}/{username}")
    public ResponseEntity<String> deleteMessage(@PathVariable Long messageId, @PathVariable Long chatId, @PathVariable String username) {
        Optional<Message> message = messageRepository.findById(messageId);
        if (message.isEmpty()) {
            return new ResponseEntity<>("the message doesnt exist!", HttpStatus.BAD_REQUEST);
        }
        if (!message.get().getChatId().equals(chatId)) {
            return new ResponseEntity<>("the message doesnt in this chat!", HttpStatus.BAD_REQUEST);
        }
        if (!message.get().getSender().equals(username)) {
            return new ResponseEntity<>("the message doesnt your message!", HttpStatus.BAD_REQUEST);
        }
        messageRepository.deleteById(messageId);
        return new ResponseEntity<>("the message deleted!", HttpStatus.OK);
    }

    @PutMapping("/messages/{messageId}/{chatId}/{username}")
    public ResponseEntity<String> editMessage(@PathVariable Long messageId, @PathVariable Long chatId, @PathVariable String username, @RequestBody String newText) {
        Optional<Message> message = messageRepository.findById(messageId);
        if (message.isEmpty()) {
            return new ResponseEntity<>("the message doesnt exist!", HttpStatus.BAD_REQUEST);
        }
        if (!message.get().getChatId().equals(chatId)) {
            return new ResponseEntity<>("the message doesnt in this chat!", HttpStatus.BAD_REQUEST);
        }
        if (!message.get().getSender().equals(username)) {
            return new ResponseEntity<>("the message doesnt your message!", HttpStatus.BAD_REQUEST);
        }
        message.get().setText(newText);
        messageRepository.save(message.get());
        return new ResponseEntity<>("the message edited!", HttpStatus.OK);
    }


}


