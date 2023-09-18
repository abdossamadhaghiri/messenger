package com.example.client;

import lombok.Setter;
import org.example.entity.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
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
    private User onlineUser;
    private Scanner scanner;
    private @Setter String usersUrl;
    private @Setter String chatsUrl;
    private @Setter String messagesUrl;
    private @Setter String invalidCommand;
    private @Setter String invalidChatId;
    private @Setter String invalidMessageId;
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
            User user = getUser(onlineUser.getUsername());
            if (user == null) {
                System.out.println(pleaseTryAgain);
                break;
            }
            onlineUser = user;
            printChatList(onlineUser.getChats());
            System.out.println("1. select chat\n2. enter a username to start chat\n3. create a group\n4. logout");
            String option = scanner.nextLine();
            if (option.equals("1")) {
                System.out.println("enter your chat id:");
                Long chatId = Long.valueOf(scanner.nextLine());
                if (isOldChat(chatId)) {
                    chat(chatId);
                }
            } else if (option.equals("2")) {
                System.out.println("enter your username:");
                String username = scanner.nextLine();
                if (isNewChat(username)) {
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
        String url = usersUrl;
        User user = new User(username);
        ResponseEntity<String> response = client.post().uri(url).bodyValue(user).retrieve()
                .onStatus(status -> status != HttpStatus.OK, clientResponse -> Mono.empty()).toEntity(String.class).block();
        return response != null ? response.getBody() : pleaseTryAgain;
    }

    private boolean signIn(String username) {
        User user = getUser(username);
        if (user != null) {
            onlineUser = user;
            System.out.println("successfully signed in");
            return true;
        }
        System.out.println("this username doesnt exist");
        return false;
    }

    private void printChatList(List<Chat> chats) {
        for (Chat chat : chats) {
            if (chat instanceof Pv pv) {
                System.out.println(chat.getId() + ". " + getPeer(pv));
            } else {
                System.out.println(chat.getId() + ". " + ((Group) chat).getName() + "(G)");
            }
        }
    }

    private String getPeer(Pv pv) {
        if (pv.getFirst().equals(onlineUser.getUsername())) {
            return pv.getSecond();
        }
        return pv.getFirst();
    }

    private void chat(Long chatId) {
        while (true) {
            Chat chat = getChat(chatId);
            if (chat == null) {
                break;
            }
            printPinnedMessage(chat.getPinMessages());
            chat.getMessages().forEach(this::printMessageInChat);
            System.out.println("1. send message\n2. delete message\n3. edit message\n4. forward message\n5. pin message\n6. back");
            String option = scanner.nextLine();
            if (option.equals("1")) {
                System.out.println("write your message:");
                String text = scanner.nextLine();
                System.out.println(writeMessage(text, chat));
            } else if (option.equals("2")) {
                System.out.println("enter your message id:");
                Long messageId = Long.valueOf(scanner.nextLine());
                System.out.println(deleteMessage(messageId, chat));
            } else if (option.equals("3")) {
                System.out.println("enter your message id:");
                Long messageId = Long.valueOf(scanner.nextLine());
                System.out.println(editMessage(messageId, chat));
            } else if (option.equals("4")) {
                System.out.println("enter your message id:");
                Long messageId = Long.valueOf(scanner.nextLine());
                forward(messageId, chat);
            } else if (option.equals("5")) {
                System.out.println("enter your message id:");
                Long messageId = Long.valueOf(scanner.nextLine());
                System.out.println(pinMessage(messageId, chat));
            } else if (option.equals("6")) {
                break;
            } else {
                System.out.println(invalidCommand);
            }
        }
    }

    private void printMessageInChat(Message message) {
        if (message.getRepliedMessageId() != 0L) {
            System.out.println(message.getId() + ". " + recognizeSender(message.getSender()) + " in reply to " + message.getRepliedMessageId() + ": " + message.getText());
        } else if (message.getForwardedFrom() != null) {
            System.out.println(message.getId() + ". " + recognizeSender(message.getSender()) + " forwarded from " + message.getForwardedFrom() + ": " + message.getText());
        } else {
            System.out.println(message.getId() + ". " + recognizeSender(message.getSender()) + ": " + message.getText());
        }
    }

    private boolean isOldChat(Long chatId) {
        if (onlineUser.getChats().stream().anyMatch(chat -> chat.getId().equals(chatId))) {
            return true;
        }
        System.out.println(invalidChatId);
        return false;
    }

    private boolean isNewChat(String username) {
        String url = chatsUrl;
        Pv pv = new Pv(onlineUser.getUsername(), username);
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
        User user = getUser(onlineUser.getUsername());
        if (user != null) {
            onlineUser = user;
            for (Chat chat : onlineUser.getChats()) {
                if (chat instanceof Pv pv && ((pv.getFirst().equals(onlineUser.getUsername()) && pv.getSecond().equals(username)) ||
                        (pv.getSecond().equals(onlineUser.getUsername()) && pv.getFirst().equals(username)))) {
                    return pv.getId();
                }
            }
        }
        return -1L;
    }

    private String recognizeSender(String sender) {
        if (sender.equals(onlineUser.getUsername())) {
            return "you";
        }
        return sender;
    }

    private String writeMessage(String text, Chat chat) {
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
        return sendMessage(text, chat, repliedMessageId, null);
    }

    private String sendMessage(String text, Chat chat, Long repliedMessageId, String forwardedFrom) {
        Message message = new Message(text, onlineUser.getUsername(), chat.getId(), repliedMessageId);
        message.setForwardedFrom(forwardedFrom);
        String url = messagesUrl;
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
                User member = getUser(username);
                if (member != null) {
                    System.out.println(username + " successfully added.");
                    members.add(username);
                } else {
                    System.out.println("this username doesnt exist");
                }
            } else if (option.equals("2")) {
                break;
            } else {
                System.out.println(invalidCommand);
            }
        }
        members.add(onlineUser.getUsername());
        String url = chatsUrl;
        Group group = new Group(onlineUser.getUsername(), members, name);
        ResponseEntity<String> response = client.post().uri(url).bodyValue(group).retrieve().toEntity(String.class).block();
        if (response != null) {
            System.out.println(response.getBody());
        } else {
            System.out.println(pleaseTryAgain);
        }
    }

    private String deleteMessage(Long messageId, Chat chat) {
        Message message = getMessage(messageId);
        if (message == null || !message.getChatId().equals(chat.getId()) || !message.getSender().equals(onlineUser.getUsername())) {
            return invalidMessageId;
        }
        String url = messagesUrl + "/" + messageId;
        ResponseEntity<String> response = client.delete().uri(url).retrieve()
                .onStatus(status -> status == HttpStatus.BAD_REQUEST, clientResponse -> Mono.empty()).toEntity(String.class).block();
        if (response == null) {
            return pleaseTryAgain;
        }
        return response.getBody();
    }

    private String editMessage(Long messageId, Chat chat) {
        Message message = getMessage(messageId);
        if (message == null || !message.getChatId().equals(chat.getId()) || !message.getSender().equals(onlineUser.getUsername())) {
            return invalidMessageId;
        }
        System.out.println("write your new message:");
        String newText = scanner.nextLine();
        message.setText(newText);
        String url = messagesUrl + "/" + messageId;
        ResponseEntity<String> response = client.put().uri(url).bodyValue(message).retrieve()
                .onStatus(status -> status != HttpStatus.OK, clientResponse -> Mono.empty()).toEntity(String.class).block();
        if (response == null) {
            return pleaseTryAgain;
        }
        return response.getBody();
    }

    private void forward(Long messageId, Chat chat) {
        Message message = getMessage(messageId);
        if (message == null || !message.getChatId().equals(chat.getId())) {
            System.out.println(invalidMessageId);
            return;
        }
        System.out.println("1. select chat\n2. enter a new username");
        String option = scanner.nextLine();
        if (option.equals("1")) {
            System.out.println("enter your chat id:");
            Long destinationChatId = Long.valueOf(scanner.nextLine());
            if (isOldChat(destinationChatId)) {
                Chat destinationChat = getChat(destinationChatId);
                System.out.println(sendMessage(message.getText(), destinationChat, 0L, message.getSender()));
            }
        } else if (option.equals("2")) {
            System.out.println("enter the username:");
            String username = scanner.nextLine();
            if (isNewChat(username)) {
                Long destinationChatId = getChatId(username);
                Chat destinationChat = getChat(destinationChatId);
                System.out.println(sendMessage(message.getText(), destinationChat, 0L, message.getSender()));
            }
        } else {
            System.out.println(invalidCommand);
        }
    }

    private String pinMessage(Long messageId, Chat chat) {
        Message pinnedMessage = getMessage(messageId);
        if (pinnedMessage == null || !pinnedMessage.getChatId().equals(chat.getId())) {
            return invalidMessageId;
        }
        chat.getPinMessages().add(pinnedMessage);
        String url = chatsUrl + "/" + chat.getId();
        ResponseEntity<String> response = client.put().uri(url).bodyValue(chat).retrieve()
                .onStatus(status -> status != HttpStatus.OK, clientResponse -> Mono.empty()).toEntity(String.class).block();
        if (response == null) {
            return pleaseTryAgain;
        }
        return "the message pinned!";
    }

    private void printPinnedMessage(List<Message> pinnedMessages) {
        if (!pinnedMessages.isEmpty()) {
            System.out.println("Pinned Messages:");
            pinnedMessages.forEach(this::printMessageInChat);
            System.out.println("----------------------------------------");
        }
    }

    private Chat getChat(Long chatId) {
        String url = chatsUrl + "/" + chatId;
        ResponseEntity<Chat> response = client.get().uri(url).retrieve()
                .onStatus(status -> status != HttpStatus.OK, clientResponse -> Mono.empty()).toEntity(Chat.class).block();
        if (response == null) {
            System.out.println(pleaseTryAgain);
            return null;
        }
        return response.getBody();
    }

    private User getUser(String username) {
        String url = usersUrl + "/" + username;
        ResponseEntity<User> response = client.get().uri(url).retrieve()
                .onStatus(status -> status != HttpStatus.OK, clientResponse -> Mono.empty()).toEntity(User.class).block();
        if (response == null) {
            System.out.println(pleaseTryAgain);
            return null;
        }
        return response.getBody();
    }

    private Message getMessage(Long messageId) {
        String url = messagesUrl + "/" + messageId;
        ResponseEntity<Message> response = client.get().uri(url).retrieve()
                .onStatus(status -> status != HttpStatus.OK, clientResponse -> Mono.empty()).toEntity(Message.class).block();
        if (response == null) {
            System.out.println(pleaseTryAgain);
            return null;
        }
        return response.getBody();
    }
}