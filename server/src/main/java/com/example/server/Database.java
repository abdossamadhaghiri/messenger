package com.example.server;

import org.example.Entity.Message;

import java.util.ArrayList;
import java.util.List;

public class Database {

    private final List<Message> allMessages;
    private final List<String> usernames;

    public Database() {
        allMessages = new ArrayList<>();
        usernames = new ArrayList<>();
    }

    public Database(List<Message> allMessages, List<String> usernames) {
        this.allMessages = allMessages;
        this.usernames = usernames;
    }

    public List<Message> getAllMessages() {
        return allMessages;
    }

    public List<String> getUsernames() {
        return usernames;
    }

    public List<Message> getMessagesInChat(String sender, String receiver) {
        List<Message> messagesInChat = new ArrayList<>();
        for (Message message : allMessages) {
            if ((message.getReceiver().equals(receiver) && message.getSender().equals(sender)) ||
                    (message.getReceiver().equals(sender) && message.getSender().equals(receiver))) {
                messagesInChat.add(message);
            }
        }
        return messagesInChat;
    }

}
