package com.example.server;

import com.example.server.repository.ChatRepository;
import com.example.server.repository.MessageRepository;
import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import org.example.entity.*;
import com.example.server.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
        if (!userRepository.existsById(username)) {
            return new ResponseEntity<>("this username doesnt exist", HttpStatus.BAD_REQUEST);
        } else {
            return new ResponseEntity<>("successfully signed in", HttpStatus.OK);
        }
    }

    @GetMapping("/chats/{username}")
    public ResponseEntity<List<Chat>> getChats(@PathVariable String username) {
        if (!userRepository.existsById(username)) {
            return new ResponseEntity<>(new ArrayList<>(), HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(chatRepository.findAll().stream().filter(chat -> isInChats(username, chat)).toList(), HttpStatus.OK);
    }

    @GetMapping("/messages/{username}/{chatId}")
    public ResponseEntity<List<Message>> getMessagesInOldChat(@PathVariable String username, @PathVariable Long chatId) {
        Optional<Chat> chat = chatRepository.findById(chatId);
        Optional<User> user = userRepository.findById(username);
        if (user.isEmpty() || chat.isEmpty() || !isInChats(username, chat.get())) {
            return new ResponseEntity<>(new ArrayList<>(), HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(messageRepository.findMessagesByChatId(chatId), HttpStatus.OK);
    }

    @PostMapping("/chat")
    public ResponseEntity<String> newChat(@RequestBody Pv pv) {
        Optional<User> firstUser = userRepository.findById(pv.getFirst());
        Optional<User> secondUser = userRepository.findById(pv.getSecond());
        if (firstUser.isEmpty() || secondUser.isEmpty()) {
            return new ResponseEntity<>("username doesnt exist.", HttpStatus.BAD_REQUEST);
        }
        if (getPvByUsernames(pv.getFirst(), pv.getSecond()) != null) {
            return new ResponseEntity<>("the chat already exists.", HttpStatus.BAD_REQUEST);
        }
        chatRepository.save(pv);
        return new ResponseEntity<>("chat started.", HttpStatus.OK);
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
    public ResponseEntity<String> newMessage(@RequestBody Message newMessage) {
        if (!userRepository.existsById(newMessage.getSender())) {
            return new ResponseEntity<>("sender username doesnt exist!", HttpStatus.BAD_REQUEST);
        }
        Optional<Chat> chat = chatRepository.findById(newMessage.getChatId());
        if (chat.isEmpty()) {
            return new ResponseEntity<>("the chat doesnt exist!", HttpStatus.BAD_REQUEST);
        }
        if (!isInChats(newMessage.getSender(), chat.get())) {
            return new ResponseEntity<>("the chat doesnt in sender's chats!", HttpStatus.BAD_REQUEST);
        }
        Optional<Message> repliedMessage = messageRepository.findById(newMessage.getRepliedMessageId());
        if (!newMessage.getRepliedMessageId().equals(0L) && repliedMessage.isEmpty()) {
            return new ResponseEntity<>("the replied message doesnt exist!", HttpStatus.BAD_REQUEST);
        }
        if (repliedMessage.isPresent() && !newMessage.getChatId().equals(repliedMessage.get().getChatId())) {
            return new ResponseEntity<>("the replied message doesnt in this chat!", HttpStatus.BAD_REQUEST);
        }
        if (newMessage.getForwardedFrom() != null && !userRepository.existsById(newMessage.getForwardedFrom())) {
            return new ResponseEntity<>("the \"forwarded from\" username doesnt exist!", HttpStatus.BAD_REQUEST);
        }
        messageRepository.save(newMessage);
        return new ResponseEntity<>("sent!", HttpStatus.OK);
    }

    @PostMapping("/groups")
    public ResponseEntity<String> newGroup(@RequestBody Group group) {
        if (group.getMembers().stream().anyMatch(username -> !userRepository.existsById(username))) {
            return new ResponseEntity<>("invalid group content!", HttpStatus.BAD_REQUEST);
        }
        chatRepository.save(group);
        return new ResponseEntity<>("the group successfully created!", HttpStatus.OK);
    }

    @DeleteMapping("/messages/{messageId}/{chatId}/{username}")
    public ResponseEntity<String> deleteMessage(@PathVariable Long messageId, @PathVariable Long chatId, @PathVariable String username) {
        Optional<Message> message = messageRepository.findById(messageId);
        Optional<Chat> chat = chatRepository.findById(chatId);
        Optional<User> user = userRepository.findById(username);
        if (user.isEmpty()) {
            return new ResponseEntity<>("the username doesnt exist!", HttpStatus.BAD_REQUEST);
        }
        if (chat.isEmpty()) {
            return new ResponseEntity<>("the chat doesnt exist!", HttpStatus.BAD_REQUEST);
        }
        if (!isInChats(username, chat.get())) {
            return new ResponseEntity<>("the chat doesnt in sender's chats!", HttpStatus.BAD_REQUEST);
        }
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
    public ResponseEntity<String> editMessage(@PathVariable Long messageId, @PathVariable Long chatId,
                                              @PathVariable String username, @RequestBody String newText) {
        Optional<Message> message = messageRepository.findById(messageId);
        Optional<Chat> chat = chatRepository.findById(chatId);
        Optional<User> user = userRepository.findById(username);
        if (user.isEmpty()) {
            return new ResponseEntity<>("the username doesnt exist!", HttpStatus.BAD_REQUEST);
        }
        if (chat.isEmpty()) {
            return new ResponseEntity<>("the chat doesnt exist!", HttpStatus.BAD_REQUEST);
        }
        if (!isInChats(username, chat.get())) {
            return new ResponseEntity<>("the chat doesnt in sender's chats!", HttpStatus.BAD_REQUEST);
        }
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

    @PostMapping("/messages/{chatId}/{username}/{destinationChatId}")
    public ResponseEntity<String> forwardMessage(@PathVariable Long chatId, @PathVariable String username,
                                                 @PathVariable Long destinationChatId, @RequestBody Long messageId) {
        Optional<Message> message = messageRepository.findById(messageId);
        Optional<Chat> destinationChat = chatRepository.findById(destinationChatId);
        Optional<Chat> chat = chatRepository.findById(chatId);
        Optional<User> user = userRepository.findById(username);
        if (user.isEmpty()) {
            return new ResponseEntity<>("the username doesnt exist!", HttpStatus.BAD_REQUEST);
        }
        if (chat.isEmpty()) {
            return new ResponseEntity<>("the chat doesnt exist!", HttpStatus.BAD_REQUEST);
        }
        if (!isInChats(username, chat.get())) {
            return new ResponseEntity<>("the chat doesnt in sender's chats!", HttpStatus.BAD_REQUEST);
        }
        if (message.isEmpty()) {
            return new ResponseEntity<>("the message doesnt exist!", HttpStatus.BAD_REQUEST);
        }
        if (!message.get().getChatId().equals(chatId)) {
            return new ResponseEntity<>("the message doesnt in this chat!", HttpStatus.BAD_REQUEST);
        }
        if (destinationChat.isEmpty()) {
            return new ResponseEntity<>("the destination chat doesnt exist!", HttpStatus.BAD_REQUEST);
        }
        if (!isInChats(username, destinationChat.get())) {
            return new ResponseEntity<>("the destination chat doesnt in sender's chats!", HttpStatus.BAD_REQUEST);
        }
        Message forwardedMessage = new Message(message.get().getText(), username, destinationChatId, 0L);
        forwardedMessage.setForwardedFrom(message.get().getSender());
        messageRepository.save(forwardedMessage);
        return new ResponseEntity<>("forwarded!", HttpStatus.OK);
    }


}


