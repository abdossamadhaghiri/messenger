package com.example.client;

import org.example.Entity.Message;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Scanner;

public class CommandProcessor {
    private static WebClient client;
    private static String onlineUsername;
    private static Scanner scanner;

    public static void run() {
        client = WebClient.create();

        scanner = new Scanner(System.in);

        while (true) {
            System.out.println("1. sign up\n2. sign in");
            String option = scanner.nextLine();
            if (option.equals("1")) {
                signUp();
            } else if (option.equals("2")) {
                if (signIn()) {
                    break;
                }
            } else {
                System.out.println("invalid command!");
            }
        }

        while (true) {
            List<String> usernamesExceptMe = getUsernamesExceptMe();
            getUsernamesExceptMe().forEach(username -> System.out.println((usernamesExceptMe.indexOf(username) + 1) + ". " + username));
            System.out.println("select your chat: (enter \"quit\" to terminate)");
            String input = scanner.nextLine();
            if (usernamesExceptMe.contains(input)) {
                chat(input);
            } else if (input.equals("quit")) {
                break;
            } else {
                System.out.println("this username doesnt exist");
            }
        }
    }

    private static void signUp() {
        System.out.println("enter your username: ");
        String username = scanner.nextLine();
        String URL = "http://localhost:9000/signUp";

        ResponseEntity<String> response = client.post().uri(URL).bodyValue(username).retrieve()
                .onStatus(status -> status == HttpStatus.BAD_REQUEST, clientResponse -> Mono.empty()).toEntity(String.class).block();
        System.out.println(response.getBody());

    }

    private static boolean signIn() {
        System.out.println("enter your username: ");
        String username = scanner.nextLine();
        String URL = "http://localhost:9000/signIn/" + username;

        ResponseEntity<String> response = client.get().uri(URL).retrieve()
                .onStatus(status -> status == HttpStatus.BAD_REQUEST, clientResponse -> Mono.empty()).toEntity(String.class).block();

        System.out.println(response.getBody());
        if (response.getStatusCode().equals(HttpStatus.OK)) {
            onlineUsername = username;
            return true;
        }
        return false;

    }

    private static List<String> getUsernamesExceptMe() {
        String URL = "http://localhost:9000/users";

        ResponseEntity<List> response = client.get().uri(URL).retrieve().toEntity(List.class).block();

        List<String> allUsernamesExceptMe = response.getBody();
        allUsernamesExceptMe.remove(onlineUsername);
        return allUsernamesExceptMe;
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
            } else {
                System.out.println("invalid command!");
            }

        }
    }

    private static void showMessages(String sender, String receiver) {
        String URL = "http://localhost:9000/messages/" + sender + "/" + receiver;

        ResponseEntity<List<Message>> response = client.get().uri(URL).retrieve().toEntity(new ParameterizedTypeReference<List<Message>>() {
        }).block();

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

    private static void sendMessage(String text, String receiverUsername) {
        Message message = new Message(text, onlineUsername, receiverUsername);
        String URL = "http://localhost:9000/messages";
        client.post().uri(URL).bodyValue(message).retrieve().toEntity(String.class).block();
    }


}
