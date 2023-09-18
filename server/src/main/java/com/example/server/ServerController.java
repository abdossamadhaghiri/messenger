package com.example.server;

import com.example.server.repository.ChatRepository;
import com.example.server.repository.MessageRepository;
import lombok.AllArgsConstructor;
import org.example.entity.*;
import com.example.server.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

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
    public ResponseEntity<List<Chat>> getChats(@PathVariable String username) {
        if (!userRepository.existsById(username)) {
            return new ResponseEntity<>(new ArrayList<>(), HttpStatus.BAD_REQUEST);
        }
        List<Chat> chats = new ArrayList<>();
        for (Chat chat : chatRepository.findAll()) {
            if (isInChats(username, chat)) {
                chats.add(chat);
            }
        }
        return new ResponseEntity<>(chats, HttpStatus.OK);
    }

    @GetMapping("/messages/{username}/{chatId}")
    public ResponseEntity<List<Message>> getMessagesInOldChat(@PathVariable String username, @PathVariable Long chatId) {
        if (!userRepository.existsById(username) || chatRepository.findById(chatId).isEmpty() || !isInChats(username, chatRepository.findById(chatId).get())) {
            return new ResponseEntity<>(new ArrayList<>(), HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(messageRepository.findMessagesByChatId(chatId), HttpStatus.OK);
    }

    @PostMapping("/chat")
    public ResponseEntity<String> newPv(@RequestBody Pv pv) {
        if (!userRepository.existsById(pv.getFirst()) || !userRepository.existsById(pv.getSecond())) {
            return new ResponseEntity<>("username doesnt exist.", HttpStatus.BAD_REQUEST);
        } else if (getPvByUsernames(pv.getFirst(), pv.getSecond()) != null) {
            return new ResponseEntity<>("the chat already exists.", HttpStatus.BAD_REQUEST);
        } else {
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
            if (chat instanceof Pv pv) {
                if ((pv.getFirst().equals(firstUsername) && pv.getSecond().equals(secondUsername)) ||
                        (pv.getFirst().equals(secondUsername) && pv.getSecond().equals(firstUsername))) {
                    return pv;
                }
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
        if (!userRepository.existsById(newMessage.getSender()) || !chatRepository.existsById(newMessage.getChatId())) {
            return new ResponseEntity<>("invalid message content!", HttpStatus.BAD_REQUEST);
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

}


