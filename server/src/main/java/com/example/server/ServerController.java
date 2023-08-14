package com.example.server;

import org.example.Message;
import org.example.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
public class ServerController {
    private final AtomicInteger counter = new AtomicInteger();
    private final Database database;

    public ServerController(@Autowired Database database) {
        this.database = database;
    }

    @GetMapping("/messages/{sender}/{receiver}")
    public List<Message> getMessagesInChat(@PathVariable String sender, @PathVariable String receiver) {
        return database.getMessagesInChat(sender, receiver);
    }


    @PostMapping("/messages")
    private Message newMessage(@RequestBody Message newMessage) {
        newMessage.setId(counter.incrementAndGet());
        database.getAllMessages().add(newMessage);
        return newMessage;
    }

    @PostMapping("/users")
    private User newUser(@RequestBody User onlineUser) {
        for (String username : database.getUsernames()) {
            if (username.equals(onlineUser.username())) {
                return onlineUser;
            }

        }
        database.getUsernames().add(onlineUser.username());
        return onlineUser;
    }

    @GetMapping("/users")
    private List<String> getUsers() {
        return database.getUsernames();
    }

}
