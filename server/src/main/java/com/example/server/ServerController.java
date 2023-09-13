package com.example.server;

import com.example.server.repository.ChatRepository;
import com.example.server.repository.MessageRepository;
import lombok.AllArgsConstructor;
import org.example.entity.*;
import com.example.server.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@AllArgsConstructor
@RestController
public class ServerController {
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final ChatRepository chatRepository;

    @PostMapping("/users")
    public ResponseEntity<String> newUser(@RequestBody User user) {
        if (userRepository.existsById(user.getUsername())) {
            return new ResponseEntity<>("this username already exists", HttpStatus.BAD_REQUEST);
        }
        userRepository.save(user);
        return new ResponseEntity<>("successfully signed up", HttpStatus.OK);
    }

    @GetMapping("/users/{username}")
    public ResponseEntity<User> getUser(@PathVariable String username) {
        Optional<User> user = userRepository.findById(username);
        return user.map(value -> new ResponseEntity<>(value, HttpStatus.OK)).orElseGet(() -> new ResponseEntity<>(null, HttpStatus.BAD_REQUEST));
    }

    @PostMapping("/chats")
    public ResponseEntity<String> newChat(@RequestBody Chat chat) {
        if (chat instanceof Pv pv) {
            return newPv(pv);
        }
        return newGroup(((Group) chat));
    }

    private ResponseEntity<String> newPv(Pv pv) {
        Optional<User> firstUser = userRepository.findById(pv.getFirst());
        Optional<User> secondUser = userRepository.findById(pv.getSecond());
        if (firstUser.isEmpty() || secondUser.isEmpty()) {
            return new ResponseEntity<>("username doesnt exist.", HttpStatus.BAD_REQUEST);
        }
        if (getPvByUsernames(pv.getFirst(), pv.getSecond()) != null) {
            return new ResponseEntity<>("the chat already exists.", HttpStatus.BAD_REQUEST);
        }
        chatRepository.save(pv);
        firstUser.get().getChats().add(pv);
        secondUser.get().getChats().add(pv);
        userRepository.save(firstUser.get());
        userRepository.save(secondUser.get());
        return new ResponseEntity<>("chat started.", HttpStatus.OK);
    }

    private ResponseEntity<String> newGroup(@RequestBody Group group) {
        if (group.getMembers().stream().anyMatch(member -> !userRepository.existsById(member))) {
            return new ResponseEntity<>("invalid group content!", HttpStatus.BAD_REQUEST);
        }
        chatRepository.save(group);
        for (String member : group.getMembers()) {
            Optional<User> user = userRepository.findById(member);
            user.get().getChats().add(group);
            userRepository.save(user.get());
        }
        return new ResponseEntity<>("the group successfully created!", HttpStatus.OK);
    }

    private boolean isInChats(String myUsername, Chat chat) {
        return userRepository.findById(myUsername).get().getChats().contains(chat);
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
//        chat.get().getMessages().add(newMessage);
//        chatRepository.save(chat.get());
        return new ResponseEntity<>("sent!", HttpStatus.OK);
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
        System.out.println(chat.get().getMessages());
        message.get().setText(newText);
        messageRepository.save(message.get());
        System.out.println("ok");
        System.out.println(chat.get().getMessages());
        return new ResponseEntity<>("the message edited!", HttpStatus.OK);
    }

    @PostMapping("/messages/{chatId}/{senderUsername}/{destinationChatId}")
    public ResponseEntity<String> forwardMessage(@PathVariable Long chatId, @PathVariable String senderUsername,
                                                 @PathVariable Long destinationChatId, @RequestBody Long messageId) {
        Optional<Message> message = messageRepository.findById(messageId);
        Optional<Chat> destinationChat = chatRepository.findById(destinationChatId);
        Optional<Chat> chat = chatRepository.findById(chatId);
        Optional<User> user = userRepository.findById(senderUsername);
        if (user.isEmpty()) {
            return new ResponseEntity<>("the senderUsername doesnt exist!", HttpStatus.BAD_REQUEST);
        }
        if (chat.isEmpty()) {
            return new ResponseEntity<>("the chat doesnt exist!", HttpStatus.BAD_REQUEST);
        }
        if (!isInChats(senderUsername, chat.get())) {
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
        if (!isInChats(senderUsername, destinationChat.get())) {
            return new ResponseEntity<>("the destination chat doesnt in sender's chats!", HttpStatus.BAD_REQUEST);
        }
        Message forwardedMessage = new Message(message.get().getText(), senderUsername, destinationChatId, 0L);
        forwardedMessage.setForwardedFrom(message.get().getSender());
        messageRepository.save(forwardedMessage);
        destinationChat.get().getMessages().add(forwardedMessage);
        chatRepository.save(destinationChat.get());
        return new ResponseEntity<>("forwarded!", HttpStatus.OK);
    }

    @GetMapping("/messages/{messageId}")
    public ResponseEntity<Message> getMessage(@PathVariable Long messageId) {
        Optional<Message> message = messageRepository.findById(messageId);
        return message.map(value -> new ResponseEntity<>(value, HttpStatus.OK)).orElseGet(() -> new ResponseEntity<>(null, HttpStatus.BAD_REQUEST));
    }

    @GetMapping("/chats/{chatId}")
    public ResponseEntity<Chat> getChat(@PathVariable Long chatId) {
        Optional<Chat> chat = chatRepository.findById(chatId);
        return chat.map(value -> new ResponseEntity<>(value, HttpStatus.OK)).orElseGet(() -> new ResponseEntity<>(null, HttpStatus.BAD_REQUEST));
    }

    @PutMapping("/chats/{chatId}")
    public ResponseEntity<String> pinMessage(@PathVariable Long chatId, @RequestBody Chat newChat) {
        Optional<Chat> chat = chatRepository.findById(chatId);
        if (chat.isEmpty()) {
            return new ResponseEntity<>("the chat doesnt exist!", HttpStatus.BAD_REQUEST);
        }
        chat.get().setPinMessages(newChat.getPinMessages());
        chatRepository.save(chat.get());
        return new ResponseEntity<>("the chat updated.", HttpStatus.OK);
    }
}