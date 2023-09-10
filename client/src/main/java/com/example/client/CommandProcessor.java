package com.example.client;

import lombok.Setter;
import org.example.entity.Chat;
import org.example.entity.Group;
import org.example.entity.Message;
import org.example.entity.Pv;
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
    private @Setter String getChatsUrl;
    private @Setter String getMessagesInOldChatUrl;
    private @Setter String enterNewChatUrl;
    private @Setter String getChatIdUrl;
    private @Setter String sendMessageUrl;
    private @Setter String deleteMessageUrl;
    private @Setter String editMessageUrl;
    private @Setter String newGroupUrl;
    private @Setter String invalidCommand;
    private @Setter String invalidId;
    private @Setter String pleaseTryAgain;

    public void run() {
        client = WebClient.create();
        scanner = new Scanner(System.in);

        while (true) {
            System.out.println("1. sign up\n2. sign in\n3. quit");
            String option = scanner.nextLine();
            if (option.equals("1")) {
                System.out.println("enter your username: ");
                String username = scanner.nextLine();
                System.out.println(signUp(username));
            } else if (option.equals("2")) {
                System.out.println("enter your username: ");
                String username = scanner.nextLine();
                if (signIn(username)) {
                    continueWithOnlineUser();
                }
            } else if (option.equals("3")) {
                break;
            } else {
                System.out.println(invalidCommand);
            }
        }
    }

    private void continueWithOnlineUser() {
        while (true) {
            List<Chat> myChats = getChats();
            showChatList(myChats);
            System.out.println("1. select chat\n2. enter a username to start chat\n3. create a group\n4. logout");
            String option = scanner.nextLine();
            if (option.equals("1")) {
                System.out.println("enter your chat id:");
                Long chatId = Long.valueOf(scanner.nextLine());
                if (canEnterOldChat(chatId)) {
                    chat(chatId);
                }
            } else if (option.equals("2")) {
                System.out.println("enter your username:");
                String username = scanner.nextLine();
                if (canStartNewChat(username)) {
                    Long chatId = getChatId(username);
                    chat(chatId);
                }
            } else if (option.equals("3")) {
                createGroup();
            } else if (option.equals("4")) {
                break;
            } else {
                System.out.println(invalidCommand);
            }
        }
    }

    private String signUp(String username) {
        String url = signUpUrl;

        ResponseEntity<String> response = client.post().uri(url).bodyValue(username).retrieve()
                .onStatus(status -> status != HttpStatus.OK, clientResponse -> Mono.empty()).toEntity(String.class).block();

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

    private List<Chat> getChats() {
        String url = getChatsUrl + onlineUsername;
        ResponseEntity<List<Chat>> response = client.get().uri(url).retrieve().toEntity(new ParameterizedTypeReference<List<Chat>>() {
        }).block();

        return response != null ? response.getBody() : new ArrayList<>();
    }

    private void showChatList(List<Chat> chats) {
        for (Chat chat : chats) {
            if (chat instanceof Pv pv) {
                System.out.println("- " + getPeer(pv) + ": " + chat.getId());
            } else {
                System.out.println("- " + ((Group) chat).getName() + "(G): " + chat.getId());
            }
        }
    }

    private String getPeer(Pv pv) {
        if (pv.getFirst().equals(onlineUsername)) {
            return pv.getSecond();
        }
        return pv.getFirst();
    }

    private void chat(Long chatId) {
        while (true) {
            getMessagesInOldChat(chatId).getBody().forEach(this::printMessageInChat);

            System.out.println("1. send message\n2. delete message\n3. edit message\n4. back");
            String option = scanner.nextLine();
            if (option.equals("1")) {
                System.out.println("write your message:");
                String text = scanner.nextLine();

                System.out.println(writeMessage(text, chatId));
            } else if (option.equals("2")) {
                System.out.println("enter your message id:");
                Long messageId = Long.valueOf(scanner.nextLine());
                deleteMessage(messageId, chatId);
            } else if (option.equals("3")) {
                System.out.println("enter your message id:");
                Long messageId = Long.valueOf(scanner.nextLine());
                editMessage(messageId, chatId);
            } else if (option.equals("4")) {
                break;
            } else {
                System.out.println(invalidCommand);
            }
        }
    }

    private void printMessageInChat(Message message) {
        if (message.getRepliedMessageId() == 0L) {
            System.err.println(message.getId() + ". " + recognizeSender(message.getSender()) + ": " + message.getText());
        } else {
            System.err.println(message.getId() + ". " + recognizeSender(message.getSender()) + " in reply to " + message.getRepliedMessageId() + ": " + message.getText());
        }
    }

    private boolean canEnterOldChat(Long chatId) {
        ResponseEntity<List<Message>> response = getMessagesInOldChat(chatId);
        if (response != null) {
            if (response.getStatusCode().equals(HttpStatus.OK)) {
                return true;
            }
            System.out.println(invalidId);
        } else {
            System.out.println(pleaseTryAgain);
        }
        return false;
    }

    private ResponseEntity<List<Message>> getMessagesInOldChat(Long chatId) {
        String url = getMessagesInOldChatUrl + onlineUsername + "/" + chatId;
        return client.get().uri(url).retrieve()
                .onStatus(status -> status != HttpStatus.OK, clientResponse -> Mono.empty()).toEntity(new ParameterizedTypeReference<List<Message>>() {
                }).block();
    }

    private boolean canStartNewChat(String username) {
        String url = enterNewChatUrl;
        Pv pv = new Pv(onlineUsername, username);
        ResponseEntity<String> response = client.post().uri(url).bodyValue(pv).retrieve()
                .onStatus(status -> status != HttpStatus.OK, clientResponse -> Mono.empty()).toEntity(String.class).block();

        if (response != null) {
            if (response.getStatusCode().equals(HttpStatus.OK)) {
                return true;
            }
            System.out.println(response.getBody());
        } else {
            System.out.println(pleaseTryAgain);
        }
        return false;
    }

    private Long getChatId(String username) {
        String url = getChatIdUrl + onlineUsername + "/" + username;
        ResponseEntity<Long> response = client.get().uri(url).retrieve()
                .onStatus(status -> status != HttpStatus.OK, clientResponse -> Mono.empty()).toEntity(Long.class).block();

        return response.getBody();
    }

    private String recognizeSender(String sender) {
        if (sender.equals(onlineUsername)) {
            return "you";
        }
        return sender;
    }

    private String writeMessage(String text, Long chatId) {
        System.out.println("1. send\n2. reply");
        String option = scanner.nextLine();
        long repliedMessageId = 0;
        if (!option.equals("1") && !option.equals("2")) {
            return invalidCommand;
        }
        if (option.equals("2")) {
            System.out.println("enter your message id:");
            repliedMessageId = Long.parseLong(scanner.nextLine());
        }
        return sendMessage(text, chatId, repliedMessageId);
    }

    private String sendMessage(String text, Long chatId, Long messageId) {
        Message message = new Message(text, onlineUsername, chatId, messageId);
        String url = sendMessageUrl;
        ResponseEntity<String> response = client.post().uri(url).bodyValue(message).retrieve()
                .onStatus(status -> status == HttpStatus.BAD_REQUEST, clientResponse -> Mono.empty()).toEntity(String.class).block();
        return response != null ? response.getBody() : pleaseTryAgain;
    }

    private void createGroup() {
        System.out.println("enter name of your group:");
        String name = scanner.nextLine();
        List<String> members = new ArrayList<>();
        while (true) {
            System.out.println("1. add member\n2. create");
            String option = scanner.nextLine();
            if (option.equals("1")) {
                System.out.println("enter the username:");
                String username = scanner.nextLine();
                String url = signInUrl + username;
                ResponseEntity<String> response = client.get().uri(url).retrieve()
                        .onStatus(status -> status != HttpStatus.OK, clientResponse -> Mono.empty()).toEntity(String.class).block();
                if (response != null) {
                    if (response.getStatusCode().equals(HttpStatus.BAD_REQUEST)) {
                        System.out.println(response.getBody());
                    } else {
                        System.out.println(username + " successfully added.");
                        members.add(username);
                    }
                } else {
                    System.out.println(pleaseTryAgain);
                }
            } else if (option.equals("2")) {
                break;
            } else {
                System.out.println(invalidCommand);
            }
        }
        members.add(onlineUsername);
        String url = newGroupUrl;
        Group group = new Group(onlineUsername, members, name);
        ResponseEntity<String> response = client.post().uri(url).bodyValue(group).retrieve().toEntity(String.class).block();
        if (response != null) {
            System.out.println(response.getBody());
        } else {
            System.out.println(pleaseTryAgain);
        }
    }

    private void deleteMessage(Long messageId, Long chatId) {
        String url = deleteMessageUrl + "/" + messageId + "/" + chatId + "/" + onlineUsername;
        ResponseEntity<String> response = client.delete().uri(url).retrieve()
                .onStatus(status -> status == HttpStatus.BAD_REQUEST, clientResponse -> Mono.empty()).toEntity(String.class).block();
        if (response != null) {
            System.out.println(response.getBody());
        } else {
            System.out.println(pleaseTryAgain);
        }

    }

    private void editMessage(Long messageId, Long chatId) {
        System.out.println("write your new message:");
        String newText = scanner.nextLine();
        String url = editMessageUrl + "/" + messageId + "/" + chatId + "/" + onlineUsername;
        ResponseEntity<String> response = client.put().uri(url).bodyValue(newText).retrieve()
                .onStatus(status -> status != HttpStatus.OK, clientResponse -> Mono.empty()).toEntity(String.class).block();
        if (response != null) {
            System.out.println(response.getBody());
        } else {
            System.out.println(pleaseTryAgain);
        }
    }
}
