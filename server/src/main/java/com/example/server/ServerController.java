package com.example.server;

import com.example.server.repository.MessageRepository;
import lombok.AllArgsConstructor;
import org.example.entity.Message;
import org.example.entity.User;
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

    @GetMapping("/users")
    public List<String> getUsers() {
        List<String> usernames = new ArrayList<>();
        userRepository.findAll().forEach(user -> usernames.add(user.getUsername()));
        return usernames;
    }

    @GetMapping("/messages/{sender}/{receiver}")
    public ResponseEntity<List<Message>> getMessagesInChat(@PathVariable String sender, @PathVariable String receiver) {
        if (!userRepository.existsById(sender) || !userRepository.existsById(receiver)) {
            return new ResponseEntity<>(new ArrayList<>(), HttpStatus.BAD_REQUEST);
        }
        List<String> senderAndReceiver = new ArrayList<>(List.of(sender, receiver));
        return new ResponseEntity<>(messageRepository.findMessagesByReceiverInAndSenderIn(senderAndReceiver, senderAndReceiver), HttpStatus.OK);
    }

    @PostMapping("/messages")
    public ResponseEntity<String> newMessage(@RequestBody Message newMessage) {
        messageRepository.save(newMessage);
        return new ResponseEntity<>("sent!", HttpStatus.OK);
    }

}


