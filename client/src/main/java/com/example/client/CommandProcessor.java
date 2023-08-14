package com.example.client;

import org.example.Message;
import org.example.User;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Scanner;

public class CommandProcessor {
    private static RestTemplate restTemplate;
    private static String onlineUsername;

    private static Scanner scanner;

    public static void run(RestTemplate restTemplate) {

        CommandProcessor.restTemplate = restTemplate;

        System.out.println("enter your name: ");
        scanner = new Scanner(System.in);
        onlineUsername = scanner.nextLine();
        signInUser();

        while (true) {
            showUsers();
            System.out.println("select your chat: ");
            String username = scanner.nextLine();
            chat(username);

        }


    }

    private static void sendMessage(String text, String receiverUsername) {
        HttpEntity<Message> request = new HttpEntity<>(new Message(text, onlineUsername, receiverUsername));
        String URL = "http://localhost:9000/messages";
        restTemplate.postForObject(URL, request, Message.class);

    }

    private static void showMessages(String sender, String receiver) {
        String URL = "http://localhost:9000/messages/" + sender + "/" + receiver;
        ResponseEntity<List<Message>> response = restTemplate.exchange(URL, HttpMethod.GET, null, new ParameterizedTypeReference<>() {
        });
        List<Message> messages = response.getBody();
        for (Message message : messages) {
            System.err.println(recognizeSender(message.getSender()) + ": " + message.getText());
        }
    }

    private static String recognizeSender(String sender) {
        if (sender.equals(onlineUsername)) {
            return "you";
        }
        return sender;
    }

    private static void signInUser() {
        HttpEntity<User> request = new HttpEntity<>(new User(onlineUsername));
        String URL = "http://localhost:9000/users";
        User newUser = restTemplate.postForObject(URL, request, User.class);

    }

    private static void showUsers() {
        String URL = "http://localhost:9000/users";
        ResponseEntity<List<String>> response = restTemplate.exchange(URL, HttpMethod.GET, null, new ParameterizedTypeReference<>() {
        });
        List<String> usernames = response.getBody();
        for (String username : usernames) {
            System.out.println((usernames.indexOf(username) + 1) + ". " + username);
        }

    }

    private static void chat(String username) {
        while (true) {
            showMessages(onlineUsername, username);
            System.out.println("1. send message\n2. back");
            String option = scanner.nextLine();
            if (option.equals("1")) {
                System.out.println("write your message:");
                String text = scanner.nextLine();
                sendMessage(text, username);
            } else if (option.equals("2")) {
                break;
            }


        }
    }

}
