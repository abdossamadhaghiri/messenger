package com.example.client;

import org.example.model.ChatModel;
import org.example.model.GroupModel;
import org.example.model.MessageModel;
import org.example.model.PvModel;
import org.example.model.UserModel;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

@Configuration
public class CommandProcessor {

    public CommandProcessor(ApiAddresses apiAddresses) {
        this.apiAddresses = apiAddresses;
    }

    private final ApiAddresses apiAddresses;
    private WebClient client;
    private UserModel onlineUser;
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
            UserModel userModel = getUser(onlineUser.getUsername());
            if (userModel == null) {
                System.out.println(Commands.PLEASE_TRY_AGAIN);
                break;
            }
            onlineUser = userModel;
            showPvs(onlineUser.getPvs());
            showGroups(onlineUser.getGroups());
            System.out.println("1. select chat by id\n2. enter pv by username\n3. create a group\n4. logout");
            String option = scanner.nextLine();
            switch (option) {
                case "1" -> selectChatById();
                case "2" -> enterPvByUsername();
                case "3" -> createGroup();
                case "4" -> flag = false;
                default -> System.out.println(Commands.INVALID_COMMAND);
            }
        }
    }

    private void selectChatById() {
        System.out.println("1. Group\n2. Pv");
        String option = scanner.nextLine();
        switch (option) {
            case "1" -> {
                System.out.println("enter your group id:");
                String input = scanner.nextLine();
                if (isNumeric(input) && isOldGroup(Long.valueOf(input))) {
                    chat(Long.valueOf(input), false);
                } else {
                    System.out.println(Commands.INVALID_CHAT_ID);
                }
            }
            case "2" -> {
                System.out.println("enter your pv id:");
                String input = scanner.nextLine();
                if (isNumeric(input) && isOldPv(Long.valueOf(input))) {
                    chat(Long.valueOf(input), true);
                } else {
                    System.out.println(Commands.INVALID_CHAT_ID);
                }
            }
            default -> System.out.println(Commands.INVALID_COMMAND);
        }
    }

    private void enterPvByUsername() {
        System.out.println("enter your username:");
        String username = scanner.nextLine();
        UserModel peer = getUser(username);
        if (peer != null) {
            Long pvId = getPvId(username);
            if (pvId != -1L) {
                chat(pvId, true);
            } else {
                System.out.println(Commands.PLEASE_TRY_AGAIN);
            }
        } else {
            System.out.println(Commands.USERNAME_DOESNT_EXIST);
        }
    }

    private String signUp(String username) {
        String url = apiAddresses.getUsersApiUrl();

        ResponseEntity<String> response = client.post()
                .uri(url)
                .bodyValue(username)
                .retrieve()
                .onStatus(status -> status != HttpStatus.OK, clientResponse -> Mono.empty())
                .toEntity(String.class)
                .block();

        return response != null ? response.getBody() : Commands.PLEASE_TRY_AGAIN;
    }

    private boolean signIn(String username) {
        UserModel userModel = getUser(username);
        if (userModel != null) {
            onlineUser = userModel;
            return true;
        }
        System.out.println(Commands.USERNAME_DOESNT_EXIST);
        return false;
    }

    private void showPvs(List<PvModel> pvs) {
        pvs.forEach(pv -> Logger.printYellow("- " + getPeer(pv) + ": " + pv.getId()));
    }

    private void showGroups(List<GroupModel> groups) {
        groups.forEach(group -> Logger.printYellow("- " + group.getName() + "(G): " + group.getId()));
    }

    private String getPeer(PvModel pv) {
        if (pv.getFirst().equals(onlineUser.getUsername())) {
            return pv.getSecond();
        }
        return pv.getFirst();
    }

    private void chat(Long chatId, boolean isPv) {
        boolean flag = true;
        while (flag) {
            ChatModel chat;
            if (isPv) {
                chat = getPv(chatId);
            } else {
                chat = getGroup(chatId);
            }
            if (chat == null) {
                System.out.println(Commands.PLEASE_TRY_AGAIN);
                break;
            }
            chat.getMessages().forEach(this::printMessageInChat);
            System.out.println("1. send message\n2. delete message\n3. edit message\n4. forward message\n5. back");
            String option = scanner.nextLine();
            switch (option) {
                case "1" -> {
                    System.out.println("write your message:");
                    String text = scanner.nextLine();
                    System.out.println(writeMessage(text, chatId));
                }
                case "2" -> {
                    System.out.println("enter your message id:");
                    String messageId = scanner.nextLine();
                    if (isNumeric(messageId)) {
                        System.out.println(deleteMessage(Long.valueOf(messageId), chatId));
                    } else {
                        System.out.println(Commands.INVALID_MESSAGE_ID);
                    }
                }
                case "3" -> {
                    System.out.println("enter your message id:");
                    String messageId = scanner.nextLine();
                    if (isNumeric(messageId)) {
                        System.out.println(editMessage(Long.valueOf(messageId), chatId));
                    } else {
                        System.out.println(Commands.INVALID_MESSAGE_ID);
                    }
                }
                case "4" -> {
                    System.out.println("enter your message id:");
                    String messageId = scanner.nextLine();
                    if (isNumeric(messageId)) {
                        forward(Long.valueOf(messageId), chatId);
                    } else {
                        System.out.println(Commands.INVALID_MESSAGE_ID);
                    }
                }
                case "5" -> flag = false;
                default -> System.out.println(Commands.INVALID_COMMAND);
            }
        }
    }

    private void printMessageInChat(MessageModel messageModel) {
        if (messageModel.getRepliedMessageId() != 0L) {
            Logger.printGreen(messageModel.getId() + ". " + recognizeSender(messageModel.getSender()) + " in reply to " + messageModel.getRepliedMessageId() + ": " + messageModel.getText());
        } else if (messageModel.getForwardedFrom() != null) {
            Logger.printGreen(messageModel.getId() + ". " + recognizeSender(messageModel.getSender()) + " forwarded from " + messageModel.getForwardedFrom() + ": " + messageModel.getText());
        } else {
            Logger.printGreen(messageModel.getId() + ". " + recognizeSender(messageModel.getSender()) + ": " + messageModel.getText());
        }
    }

    private boolean isNumeric(String input) {
        return Pattern.matches("\\d+", input);
    }

    private boolean isOldPv(Long pvId) {
        return onlineUser.getPvs().stream().anyMatch(pv -> pv.getId().equals(pvId));
    }

    private boolean isOldGroup(Long groupId) {
        return onlineUser.getGroups().stream().anyMatch(group -> group.getId().equals(groupId));
    }

    private Long getPvId(String username) {
        for (PvModel pv : onlineUser.getPvs()) {
            if (getPeer(pv).equals(username)) {
                return pv.getId();
            }
        }
        PvModel newPv = createNewPv(username);
        return newPv != null ? newPv.getId() : -1L;
    }

    private PvModel createNewPv(String peerUsername) {
        PvModel pvModel = PvModel.builder().first(onlineUser.getUsername()).second(peerUsername).build();
        ResponseEntity<PvModel> response = client.post()
                .uri(apiAddresses.getPvsApiUrl())
                .bodyValue(pvModel)
                .retrieve()
                .toEntity(PvModel.class)
                .block();
        return response != null ? response.getBody() : null;
    }

    private String recognizeSender(String sender) {
        if (sender.equals(onlineUser.getUsername())) {
            return "you";
        }
        return sender;
    }

    private String writeMessage(String text, Long chatId) {
        System.out.println("1. send\n2. reply");
        String option = scanner.nextLine();
        long repliedMessageId = 0;
        switch (option) {
            case "1" -> {
                return sendMessage(text, chatId, repliedMessageId, null);
            }
            case "2" -> {
                System.out.println("enter your message id:");
                String input = scanner.nextLine();
                if (!isNumeric(input)) {
                    return Commands.INVALID_MESSAGE_ID;
                }
                repliedMessageId = Long.parseLong(input);
                return sendMessage(text, chatId, repliedMessageId, null);
            }
            default -> {
                return Commands.INVALID_COMMAND;
            }
        }
    }

    private String sendMessage(String text, Long chatId, Long repliedMessageId, String forwardedFrom) {
        MessageModel messageModel = MessageModel.builder()
                .text(text)
                .sender(onlineUser.getUsername())
                .chatId(chatId)
                .repliedMessageId(repliedMessageId)
                .forwardedFrom(forwardedFrom)
                .build();
        String url = apiAddresses.getMessagesApiUrl();
        ResponseEntity<String> response = client.post()
                .uri(url)
                .bodyValue(messageModel)
                .retrieve()
                .onStatus(status -> status == HttpStatus.BAD_REQUEST, clientResponse -> Mono.empty())
                .toEntity(String.class)
                .block();
        return response != null ? response.getBody() : Commands.PLEASE_TRY_AGAIN;
    }

    private void createGroup() {
        System.out.println("enter name of your group:");
        String name = scanner.nextLine();
        List<String> members = new ArrayList<>();
        members.add(onlineUser.getUsername());
        boolean flag = true;
        while (flag) {
            System.out.println("1. add member\n2. create");
            String option = scanner.nextLine();
            switch (option) {
                case "1" -> {
                    System.out.println("enter the username:");
                    String username = scanner.nextLine();
                    UserModel userModel = getUser(username);
                    if (userModel == null) {
                        System.out.println(Commands.USERNAME_DOESNT_EXIST);
                    } else if (members.contains(username)) {
                        System.out.println("already joined!");
                    } else {
                        System.out.println(username + " successfully added.");
                        members.add(username);
                    }
                }
                case "2" -> flag = false;
                default -> System.out.println(Commands.INVALID_COMMAND);
            }
        }
        String url = apiAddresses.getGroupsApiUrl();
        GroupModel group = GroupModel.builder().owner(onlineUser.getUsername()).members(members).name(name).build();
        ResponseEntity<GroupModel> response = client.post()
                .uri(url)
                .bodyValue(group)
                .retrieve()
                .toEntity(GroupModel.class)
                .block();
        if (response != null) {
            System.out.println("the group successfully created!");
        } else {
            System.out.println(Commands.PLEASE_TRY_AGAIN);
        }
    }

    private String deleteMessage(Long messageId, Long chatId) {
        MessageModel messageModel = getMessage(messageId);
        if (messageModel == null || !messageModel.getChatId().equals(chatId) || !messageModel.getSender().equals(onlineUser.getUsername())) {
            return Commands.INVALID_MESSAGE_ID;
        }
        String url = apiAddresses.getMessagesApiUrl() + File.separator + messageId;
        ResponseEntity<String> response = client.delete()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, onlineUser.getToken())
                .retrieve()
                .onStatus(status -> status == HttpStatus.BAD_REQUEST, clientResponse -> Mono.empty())
                .toEntity(String.class)
                .block();
        if (response == null) {
            return Commands.PLEASE_TRY_AGAIN;
        }
        return response.getBody();
    }

    private String editMessage(Long messageId, Long chatId) {
        MessageModel messageModel = getMessage(messageId);
        if (messageModel == null || !messageModel.getChatId().equals(chatId) || !messageModel.getSender().equals(onlineUser.getUsername())) {
            return Commands.INVALID_MESSAGE_ID;
        }
        System.out.println("write your new message:");
        String newText = scanner.nextLine();
        MessageModel newMessageModel = messageModel.toBuilder().text(newText).build();
        String url = apiAddresses.getMessagesApiUrl() + File.separator + messageId;
        ResponseEntity<String> response = client.put()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, onlineUser.getToken()).bodyValue(newMessageModel)
                .retrieve()
                .onStatus(status -> status != HttpStatus.OK, clientResponse -> Mono.empty())
                .toEntity(String.class)
                .block();
        if (response == null) {
            return Commands.PLEASE_TRY_AGAIN;
        }
        return response.getBody();
    }

    private void forward(Long messageId, Long chatId) {
        MessageModel messageModel = getMessage(messageId);
        if (messageModel == null || !messageModel.getChatId().equals(chatId)) {
            System.out.println(Commands.INVALID_MESSAGE_ID);
            return;
        }
        System.out.println("1. select chat by id\n2. select pv by username");
        String option = scanner.nextLine();
        switch (option) {
            case "1" -> {
                System.out.println("1. Group\n2. Pv");
                String option2 = scanner.nextLine();
                switch (option2) {
                    case "1" -> {
                        System.out.println("enter your group id:");
                        String input = scanner.nextLine();
                        if (isNumeric(input) && isOldGroup(Long.valueOf(input))) {
                            Long destinationGroupId = Long.valueOf(input);
                            System.out.println(sendMessage(messageModel.getText(), destinationGroupId, 0L, messageModel.getSender()));
                        } else {
                            System.out.println(Commands.INVALID_CHAT_ID);
                        }
                    }
                    case "2" -> {
                        System.out.println("enter your pv id:");
                        String input = scanner.nextLine();
                        if (isNumeric(input) && isOldPv(Long.valueOf(input))) {
                            Long destinationPvId = Long.valueOf(input);
                            System.out.println(sendMessage(messageModel.getText(), destinationPvId, 0L, messageModel.getSender()));
                        } else {
                            System.out.println(Commands.INVALID_CHAT_ID);
                        }
                    }
                    default -> System.out.println(Commands.INVALID_COMMAND);
                }
            }
            case "2" -> {
                System.out.println("enter the username:");
                String username = scanner.nextLine();
                UserModel peer = getUser(username);
                if (peer != null) {
                    Long destinationPvId = getPvId(username);
                    if (destinationPvId != -1L) {
                        System.out.println(sendMessage(messageModel.getText(), destinationPvId, 0L, messageModel.getSender()));
                    } else {
                        System.out.println(Commands.PLEASE_TRY_AGAIN);
                    }
                } else {
                    System.out.println(Commands.USERNAME_DOESNT_EXIST);
                }
            }
            default -> System.out.println(Commands.INVALID_COMMAND);
        }
    }

    private MessageModel getMessage(Long messageId) {
        String url = apiAddresses.getMessagesApiUrl() + File.separator + messageId;
        ResponseEntity<MessageModel> response = client.get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, onlineUser.getToken())
                .retrieve()
                .onStatus(status -> status != HttpStatus.OK, clientResponse -> Mono.empty())
                .toEntity(MessageModel.class)
                .block();
        if (response == null) {
            System.out.println(Commands.PLEASE_TRY_AGAIN);
            return null;
        }
        return response.getBody();
    }

    private UserModel getUser(String username) {
        String url = apiAddresses.getUsersApiUrl() + File.separator + username;
        ResponseEntity<UserModel> response = client.get()
                .uri(url)
                .retrieve()
                .onStatus(status -> status != HttpStatus.OK, clientResponse -> Mono.empty())
                .toEntity(UserModel.class)
                .block();
        if (response == null) {
            System.out.println(Commands.PLEASE_TRY_AGAIN);
            return null;
        }
        return response.getBody();
    }

    private PvModel getPv(Long pvId) {
        String url = apiAddresses.getPvsApiUrl() + File.separator + pvId;
        ResponseEntity<PvModel> response = client.get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, onlineUser.getToken())
                .retrieve()
                .onStatus(status -> status != HttpStatus.OK, clientResponse -> Mono.empty())
                .toEntity(PvModel.class)
                .block();
        if (response == null) {
            System.out.println(Commands.PLEASE_TRY_AGAIN);
            return null;
        }
        return response.getBody();
    }

    private GroupModel getGroup(Long groupId) {
        String url = apiAddresses.getGroupsApiUrl() + File.separator + groupId;
        ResponseEntity<GroupModel> response = client.get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, onlineUser.getToken())
                .retrieve()
                .onStatus(status -> status != HttpStatus.OK, clientResponse -> Mono.empty())
                .toEntity(GroupModel.class)
                .block();
        if (response == null) {
            System.out.println(Commands.PLEASE_TRY_AGAIN);
            return null;
        }
        return response.getBody();
    }

}
