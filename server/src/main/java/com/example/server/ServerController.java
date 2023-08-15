package com.example.server;


import com.example.server.Repository.MessageRepository;
import org.example.Entity.Message;
import org.example.Entity.User;
import com.example.server.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
public class ServerController {
    private final AtomicInteger counter = new AtomicInteger();
    private final UserRepository userRepository;

    private final MessageRepository messageRepository;

    @Autowired
    public ServerController(UserRepository userRepository, MessageRepository messageRepository) {
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
    }

    @GetMapping("/messages/{sender}/{receiver}")
    public List<Message> getMessagesInChat(@PathVariable String sender, @PathVariable String receiver) {
        List<String> senderAndReceiver = new ArrayList<>();
        senderAndReceiver.add(sender);
        senderAndReceiver.add(receiver);
        return messageRepository.findMessagesByReceiverInAndSenderIn(senderAndReceiver, senderAndReceiver);
    }


    @PostMapping("/messages")
    private Message newMessage(@RequestBody Message newMessage) {
        newMessage.setId(counter.incrementAndGet());
        messageRepository.save(newMessage);
        return newMessage;
    }

    @PostMapping("/users")
    private User newUser(@RequestBody User onlineUser) {
        for (User user : userRepository.findAll()) {
            if (user.getUsername().equals(onlineUser.getUsername())) {
                return onlineUser;
            }

        }
        userRepository.save(onlineUser);
        return onlineUser;
    }

    @GetMapping("/users")
    private List<String> getUsers() {
        List<String> usernames = new ArrayList<>();
        userRepository.findAll().forEach(user -> usernames.add(user.getUsername()));
        return usernames;
    }

}


