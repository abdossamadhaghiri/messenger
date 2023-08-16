package com.example.server;

import com.example.server.Repository.MessageRepository;
import lombok.AllArgsConstructor;
import org.example.Entity.Message;
import org.example.Entity.User;
import com.example.server.Repository.UserRepository;
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

    @GetMapping("/messages/{sender}/{receiver}")
    public List<Message> getMessagesInChat(@PathVariable String sender, @PathVariable String receiver) {
        List<String> senderAndReceiver = new ArrayList<>(List.of(sender, receiver));
        return messageRepository.findMessagesByReceiverInAndSenderIn(senderAndReceiver, senderAndReceiver);
    }


    @PostMapping("/messages")
    private void newMessage(@RequestBody Message newMessage) {
        messageRepository.save(newMessage);
    }

    @GetMapping("/users")
    private List<String> getUsers() {
        List<String> usernames = new ArrayList<>();
        userRepository.findAll().forEach(user -> usernames.add(user.getUsername()));
        return usernames;
    }

    @PostMapping("/signUp")
    private ResponseEntity<String> signUp(@RequestBody String username) {
        if (userRepository.existsById(username)) {
            return new ResponseEntity<>("this username already exists", HttpStatus.BAD_REQUEST);
        }
        userRepository.save(new User(username));
        return new ResponseEntity<>("successfully signed up", HttpStatus.OK);
    }

    @GetMapping("signIn/{username}")
    private ResponseEntity<String> signIn(@PathVariable String username) {
        if (userRepository.existsById(username)) {
            return new ResponseEntity<>("successfully signed in", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("this username doesnt exist", HttpStatus.BAD_REQUEST);
        }
    }

}


