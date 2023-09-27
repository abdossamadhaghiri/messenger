package com.example.client;

import org.example.model.ChatModel;
import org.example.model.GroupModel;
import org.example.model.MessageModel;
import org.example.model.PvModel;
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
public class CommandProcessor {

    public CommandProcessor(ApiAddresses apiAddresses) {
        this.apiAddresses = apiAddresses;
    }

    private final ApiAddresses apiAddresses;
    private WebClient client;
    private String onlineUsername;
    private Scanner scanner;

    public void run() {
        client = WebClient.create();
        scanner = new Scanner(System.in);

        boolean flag = true;
        while (flag) {
            System.out.println("1. sign up\n2. sign in\n3. quit");
            String option = scanner.nextLine();
            switch (option) {
                case "1" -> {
                    System.out.println("enter your username: ");
                    String username = scanner.nextLine();
                    System.out.println(signUp(username));
                }
                case "2" -> {
                    System.out.println("enter your username: ");
                    String username = scanner.nextLine();
                    if (signIn(username)) {
                        continueWithOnlineUser();
                    }
                }
                case "3" -> flag = false;
                default -> System.out.println(Commands.INVALID_COMMAND);
            }
        }
    }

    private void continueWithOnlineUser() {
        boolean flag = true;
        while (flag) {
            List<ChatModel> myChats = getChats();
            showChatList(myChats);
            System.out.println("1. select chat\n2. enter a username to start chat\n3. create a group\n4. logout");
            String option = scanner.nextLine();
            switch (option) {
                case "1" -> {
                    System.out.println("enter your chat id:");
                    Long chatId = Long.valueOf(scanner.nextLine());
                    if (canEnterOldChat(chatId)) {
                        chat(chatId);
                    } else {
                        System.out.println("invalid id!");
                    }
                }
                case "2" -> {
                    System.out.println("enter your username:");
                    String username = scanner.nextLine();
                    if (canStartNewChat(username)) {
                        Long newChatId = getChatId(username);
                        chat(newChatId);
                    } else {
                        System.out.println("the username doesnt exist!");
                    }
                }
                case "3" -> createGroup();
                case "4" -> flag = false;
                default -> System.out.println(Commands.INVALID_COMMAND);
            }
        }
    }

    private String signUp(String username) {
        String url = apiAddresses.getSignUpApiUrl();

        ResponseEntity<String> response = client.post().uri(url).bodyValue(username).retrieve()
                .onStatus(status -> status != HttpStatus.OK, clientResponse -> Mono.empty()).toEntity(String.class).block();

        return response != null ? response.getBody() : Commands.PLEASE_TRY_AGAIN;
    }

    private boolean signIn(String username) {
        String url = apiAddresses.getSignInApiUrl() + username;

        ResponseEntity<String> response = client.get().uri(url).retrieve()
                .onStatus(status -> status != HttpStatus.OK, clientResponse -> Mono.empty()).toEntity(String.class).block();

        if (response != null) {
            System.out.println(response.getBody());
            if (response.getStatusCode().equals(HttpStatus.OK)) {
                onlineUsername = username;
                return true;
            }
        } else {
            System.out.println(Commands.PLEASE_TRY_AGAIN);
        }
        return false;
    }

    private List<ChatModel> getChats() {
        String url = apiAddresses.getGetChatsApiUrl() + onlineUsername;
        ResponseEntity<List<ChatModel>> response = client.get().uri(url).retrieve().toEntity(new ParameterizedTypeReference<List<ChatModel>>() {
        }).block();

        return response != null ? response.getBody() : new ArrayList<>();
    }

    private void showChatList(List<ChatModel> chats) {
        for (ChatModel chat : chats) {
            if (chat instanceof PvModel pv) {
                System.out.println("- " + getPeer(pv) + ": " + chat.getId());
            } else {
                System.out.println("- " + ((GroupModel) chat).getName() + "(G): " + chat.getId());
            }
        }
    }

    private String getPeer(PvModel pv) {
        if (pv.getFirst().equals(onlineUsername)) {
            return pv.getSecond();
        }
        return pv.getFirst();
    }

    private void chat(Long chatId) {
        boolean flag = true;
        while (flag) {
            getMessagesInOldChat(chatId).getBody().forEach(message ->
                    System.err.println(message.getId() + ". " + recognizeSender(message.getSender()) + ": " + message.getText()));

            System.out.println("1. send message\n2. delete message\n3. edit message\n4. back");
            String option = scanner.nextLine();
            switch (option) {
                case "1" -> {
                    System.out.println("write your message:");
                    String text = scanner.nextLine();
                    System.out.println(sendMessage(text, chatId));
                }
                case "2" -> {
                    System.out.println("enter your message id:");
                    Long messageId = Long.valueOf(scanner.nextLine());
                    System.out.println(deleteMessage(messageId, chatId));
                }
                case "3" -> {
                    System.out.println("enter your message id:");
                    Long messageId = Long.valueOf(scanner.nextLine());
                    System.out.println(editMessage(messageId, chatId));
                }
                case "4" -> flag = false;
                default -> System.out.println(Commands.INVALID_COMMAND);
            }
        }
    }

    private boolean canEnterOldChat(Long chatId) {
        ResponseEntity<List<MessageModel>> response = getMessagesInOldChat(chatId);
        return response != null && response.getStatusCode().equals(HttpStatus.OK);
    }

    private ResponseEntity<List<MessageModel>> getMessagesInOldChat(Long chatId) {
        String url = apiAddresses.getGetMessagesInOldChatApiUrl() + onlineUsername + "/" + chatId;
        return client.get().uri(url).retrieve()
                .onStatus(status -> status != HttpStatus.OK, clientResponse -> Mono.empty()).toEntity(new ParameterizedTypeReference<List<MessageModel>>() {
                }).block();
    }

    private boolean canStartNewChat(String username) {
        String url = apiAddresses.getEnterNewChatApiUrl();
        PvModel pv = new PvModel(onlineUsername, username);
        ResponseEntity<String> response = client.post().uri(url).bodyValue(pv).retrieve()
                .onStatus(status -> status != HttpStatus.OK, clientResponse -> Mono.empty()).toEntity(String.class).block();

        return response != null && response.getStatusCode().equals(HttpStatus.OK);
    }

    private Long getChatId(String username) {
        String url = apiAddresses.getGetChatIdApiUrl() + onlineUsername + "/" + username;
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

    private String sendMessage(String text, Long chatId) {
        MessageModel messageModel = new MessageModel(text, onlineUsername, chatId);
        String url = apiAddresses.getSendMessageApiUrl();
        ResponseEntity<String> response = client.post().uri(url).bodyValue(messageModel).retrieve().toEntity(String.class).block();
        return response != null ? response.getBody() : Commands.PLEASE_TRY_AGAIN;
    }

    private void createGroup() {
        System.out.println("enter name of your group:");
        String name = scanner.nextLine();
        List<String> members = new ArrayList<>();
        boolean flag = true;
        while (flag) {
            System.out.println("1. add member\n2. create");
            String option = scanner.nextLine();
            if (option.equals("1")) {
                System.out.println("enter the username:");
                String username = scanner.nextLine();
                String url = apiAddresses.getSignInApiUrl() + username;
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
                    System.out.println(Commands.PLEASE_TRY_AGAIN);
                }
            } else if (option.equals("2")) {
                flag = false;
            } else {
                System.out.println(Commands.INVALID_COMMAND);
            }
        }
        members.add(onlineUsername);
        String url = apiAddresses.getNewGroupApiUrl();
        GroupModel group = new GroupModel(onlineUsername, members, name);
        ResponseEntity<String> response = client.post().uri(url).bodyValue(group).retrieve().toEntity(String.class).block();
        if (response != null) {
            System.out.println(response.getBody());
        } else {
            System.out.println(Commands.PLEASE_TRY_AGAIN);
        }
    }

    private String deleteMessage(Long messageId, Long chatId) {
        MessageModel messageModel = getMessage(messageId);
        if (messageModel == null || !messageModel.getChatId().equals(chatId) || !messageModel.getSender().equals(onlineUsername)) {
            return Commands.INVALID_MESSAGE_ID;
        }
        String url = apiAddresses.getDeleteMessageApiUrl() + "/" + messageId;
        ResponseEntity<String> response = client.delete().uri(url).retrieve()
                .onStatus(status -> status == HttpStatus.BAD_REQUEST, clientResponse -> Mono.empty()).toEntity(String.class).block();
        if (response == null) {
            return Commands.PLEASE_TRY_AGAIN;
        }
        return response.getBody();
    }

    private String editMessage(Long messageId, Long chatId) {
        MessageModel messageModel = getMessage(messageId);
        if (messageModel == null || !messageModel.getChatId().equals(chatId) || !messageModel.getSender().equals(onlineUsername)) {
            return Commands.INVALID_MESSAGE_ID;
        }
        System.out.println("write your new message:");
        String newText = scanner.nextLine();
        messageModel.setText(newText);
        String url = apiAddresses.getEditMessageApiUrl() + "/" + messageId;
        ResponseEntity<String> response = client.put().uri(url).bodyValue(messageModel).retrieve()
                .onStatus(status -> status != HttpStatus.OK, clientResponse -> Mono.empty()).toEntity(String.class).block();
        if (response == null) {
            return Commands.PLEASE_TRY_AGAIN;
        }
        return response.getBody();
    }

    private MessageModel getMessage(Long messageId) {
        String url = apiAddresses.getGetMessageApiUrl() + "/" + messageId;
        ResponseEntity<MessageModel> response = client.get().uri(url).retrieve()
                .onStatus(status -> status != HttpStatus.OK, clientResponse -> Mono.empty()).toEntity(MessageModel.class).block();
        if (response == null) {
            System.out.println(Commands.PLEASE_TRY_AGAIN);
            return null;
        }
        return response.getBody();
    }

}
