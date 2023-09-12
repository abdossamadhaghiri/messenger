package com.example.client;

import lombok.Setter;
import org.example.entity.Message;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

@Configuration
@ConfigurationProperties(prefix = "const")
public class CommandProcessor {
    private WebClient client;
    private String onlineUsername;
    private Scanner scanner;

    private @Setter String signUpUrl;
    private @Setter String signInUrl;
    private @Setter String getUsersUrl;
    private @Setter String getMessagesUrl;
    private @Setter String sendMessageUrl;
    private @Setter String invalidCommand;
    private @Setter String pleaseTryAgain;

    public void run() {
        client = WebClient.create();
        scanner = new Scanner(System.in);

        while (true) {
            System.out.println("1. sign up\n2. sign in");
            String option = scanner.nextLine();
            if (option.equals("1")) {
                System.out.println("enter your username: ");
                String username = scanner.nextLine();
                System.out.println(signUp(username));
            } else if (option.equals("2")) {
                System.out.println("enter your username: ");
                String username = scanner.nextLine();
                if (signIn(username)) {
                    break;
                }
            } else {
                System.out.println(invalidCommand);
            }
        }
        continueWithOnlineUser();
    }

    private void continueWithOnlineUser() {
        while (true) {
            List<String> usernamesExceptMe = getUsernamesExceptMe();
            usernamesExceptMe.forEach(username -> System.out.println("- " + username));
            System.out.println("1. select chat\n2. quit");
            String option = scanner.nextLine();
            if (option.equals("1")) {
                System.out.println("select your chat:");
                String peer = scanner.nextLine();
                if (usernamesExceptMe.contains(peer)) {
                    chat(peer);
                } else {
                    System.out.println("this username doesnt exist");
                }
            } else if (option.equals("2")){
                break;
            } else {
                System.out.println(invalidCommand);
            }
        }
    }

    private String signUp(String username) {
        String url = signUpUrl;

        ResponseEntity<String> response = client.post().uri(url).bodyValue(username).retrieve()
                .onStatus(status -> status == HttpStatus.BAD_REQUEST, clientResponse -> Mono.empty()).toEntity(String.class).block();

        return response != null ? response.getBody() : pleaseTryAgain;

    }

    private boolean signIn(String username) {
        String url = signInUrl + username;

        ResponseEntity<String> response = client.get().uri(url).retrieve()
                .onStatus(status -> status != HttpStatus.OK, clientResponse -> Mono.empty()).toEntity(String.class).block();

        if (response != null) {
            System.out.println(response.getBody());
            if (response.getStatusCode().equals(HttpStatus.OK)) {
                onlineUsername = username;
                return true;
            }
        } else {
            System.out.println(pleaseTryAgain);
        }
        return false;
    }

    private List<String> getUsernames() {
        String url = getUsersUrl;
        ResponseEntity<List<String>> response = client.get().uri(url).retrieve().toEntity(new ParameterizedTypeReference<List<String>>() {
        }).block();

        return response != null ? response.getBody() : new ArrayList<>();
    }

    private List<String> getUsernamesExceptMe() {
        List<String> usernamesExceptMe = getUsernames();
        usernamesExceptMe.remove(onlineUsername);
        return usernamesExceptMe;
    }

    private void chat(String username) {
        while (true) {
            getMessagesInChat(onlineUsername, username).forEach(message ->
                    System.err.println(recognizeSender(message.getSender()) + ": " + message.getText()));
            System.out.println("1. send message\n2. back");
            String option = scanner.nextLine();
            if (option.equals("1")) {
                System.out.println("write your message:");
                String text = scanner.nextLine();
                System.out.println(sendMessage(text, username));
            } else if (option.equals("2")) {
                break;
            } else {
                System.out.println(invalidCommand);
            }
        }
    }

    private List<Message> getMessagesInChat(String sender, String receiver) {
        String url = getMessagesUrl + sender + "/" + receiver;
        ResponseEntity<List<Message>> response = client.get().uri(url).retrieve().toEntity(new ParameterizedTypeReference<List<Message>>() {
        }).block();

        return response != null ? response.getBody() : new ArrayList<>();
    }

    private String recognizeSender(String sender) {
        if (sender.equals(onlineUsername)) {
            return "you";
        }
        return sender;
    }

    private String sendMessage(String text, String receiverUsername) {
        Message message = new Message(text, onlineUsername, receiverUsername);
        String url = sendMessageUrl;
        ResponseEntity<String> response = client.post().uri(url).bodyValue(message).retrieve().toEntity(String.class).block();
        return response != null ? response.getBody() : pleaseTryAgain;
    }


}
