package com.example.client;

import org.example.UrlPaths;
import org.example.model.GroupMessageModel;
import org.example.model.GroupModel;
import org.example.model.MessageModel;
import org.example.model.PvMessageModel;
import org.example.model.PvModel;
import org.example.model.UserModel;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

import static org.example.UrlPaths.SLASH;

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
        client = WebClient.create(apiAddresses.getBaseUrl());
        scanner = new Scanner(System.in);

        boolean flag = true;
        while (flag) {
            Logger.print("1. sign up\n2. sign in\n3. quit");
            String option = scanner.nextLine();
            switch (option) {
                case "1" -> {
                    Logger.print("enter your username: ");
                    String username = scanner.nextLine();
                    Logger.printRed(signUp(username));
                }
                case "2" -> {
                    Logger.print("enter your username: ");
                    String username = scanner.nextLine();
                    if (signIn(username)) {
                        continueWithOnlineUser();
                    }
                }
                case "3" -> flag = false;
                default -> Logger.printRed(Commands.INVALID_COMMAND);
            }
        }
    }

    private void continueWithOnlineUser() {
        boolean flag = true;
        while (flag) {
            Disposable pvNotifications = getPvNotificationsInMainMenu();
            Disposable groupNotifications = getGroupNotificationsInMainMenu();
            UserModel userModel = getUser(onlineUser.getUsername());
            if (userModel == null) {
                Logger.printRed(Commands.PLEASE_TRY_AGAIN);
                break;
            }
            onlineUser = userModel;
            showPvs(onlineUser.getPvs());
            showGroups(onlineUser.getGroups());
            Logger.print("1. select chat by id\n2. enter pv by username\n3. create a group\n4. logout");
            String option = scanner.nextLine();
            switch (option) {
                case "1" -> selectChatById();
                case "2" -> enterPvByUsername();
                case "3" -> createGroup();
                case "4" -> {
                    flag = false;
                    pvNotifications.dispose();
                    groupNotifications.dispose();
                    signOut(onlineUser.getUsername());
                }
                default -> Logger.printRed(Commands.INVALID_COMMAND);
            }
        }
    }

    private Disposable getPvNotificationsInMainMenu() {
        Flux<PvMessageModel> flux = client.get()
                .uri("pvNotifications" + SLASH + onlineUser.getUsername())
                .retrieve()
                .bodyToFlux(PvMessageModel.class);
        return flux.subscribe(pvMessageModel -> Logger.printGreen("a new message from " + pvMessageModel.getSender() + " received!"));
    }

    private Disposable getGroupNotificationsInMainMenu() {
        Flux<GroupMessageModel> flux = client.get()
                .uri("groupNotifications" + SLASH + onlineUser.getUsername())
                .retrieve()
                .bodyToFlux(GroupMessageModel.class);
        return flux.subscribe(groupMessageModel -> Logger.printGreen("a new message from "
                + groupMessageModel.getSender() + " in group with id " + groupMessageModel.getGroupId() + " received!"));
    }

    private void signOut(String username) {
        ResponseEntity<String> response = client.get()
                .uri("signOut" + SLASH + username)
                .retrieve()
                .onStatus(status -> status != HttpStatus.OK, clientResponse -> Mono.empty())
                .toEntity(String.class)
                .block();
        if (response != null) {
            Logger.printRed(response.getBody());
        } else {
            Logger.printRed(Commands.PLEASE_TRY_AGAIN);
        }
    }

    private void selectChatById() {
        Logger.print("1. Group\n2. Pv");
        String option = scanner.nextLine();
        switch (option) {
            case "1" -> {
                Logger.print("enter your group id:");
                String input = scanner.nextLine();
                if (isNumeric(input) && isOldGroup(Long.valueOf(input))) {
                    groupChat(Long.valueOf(input));
                } else {
                    Logger.printRed(Commands.INVALID_CHAT_ID);
                }
            }
            case "2" -> {
                Logger.print("enter your pv id:");
                String input = scanner.nextLine();
                if (isNumeric(input) && isOldPv(Long.valueOf(input))) {
                    pvChat(Long.valueOf(input));
                } else {
                    Logger.printRed(Commands.INVALID_CHAT_ID);
                }
            }
            default -> Logger.printRed(Commands.INVALID_COMMAND);
        }
    }

    private void enterPvByUsername() {
        Logger.print("enter your username:");
        String username = scanner.nextLine();
        UserModel peer = getUser(username);
        if (peer != null) {
            if (!peer.getUsername().equals(onlineUser.getUsername())) {
                Long pvId = getPvId(username);
                if (pvId != -1L) {
                    pvChat(pvId);
                } else {
                    Logger.printRed(Commands.PLEASE_TRY_AGAIN);
                }
            }
            else {
                Logger.printRed(Commands.ITS_YOUR_USERNAME);
            }
        } else {
            Logger.printRed(Commands.USERNAME_DOESNT_EXIST);
        }
    }

    private String signUp(String username) {
        String url = UrlPaths.SIGN_UP_URL_PATH;
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
        String url = UrlPaths.SIGN_IN_URL_PATH + SLASH + username;
        ResponseEntity<UserModel> response = client.get()
                .uri(url)
                .retrieve()
                .onStatus(status -> status != HttpStatus.OK, clientResponse -> Mono.empty())
                .toEntity(UserModel.class)
                .block();
        if (response != null) {
            if (response.getStatusCode().equals(HttpStatus.OK)) {
                onlineUser = response.getBody();
                return true;
            } else if (response.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                Logger.printRed(Commands.USERNAME_DOESNT_EXIST);
            } else {
                Logger.printRed(Commands.THIS_USER_IS_ONLINE_NOW);
            }
        } else {
            Logger.printRed(Commands.PLEASE_TRY_AGAIN);
        }
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

    private void pvChat(Long pvId) {
        Disposable pvNotifications = getPvNotificationsInChat(pvId);
        boolean flag = true;
        while (flag) {
            PvModel pv = getPv(pvId);
            if (pv == null) {
                Logger.printRed(Commands.PLEASE_TRY_AGAIN);
                break;
            }
            pv.getPvMessages().forEach(this::printMessageInChat);
            Logger.print("1. send message\n2. delete message\n3. edit message\n4. forward message\n5. back");
            String option = scanner.nextLine();
            switch (option) {
                case "1" -> {
                    Logger.print("write your message:");
                    String text = scanner.nextLine();
                    Logger.printRed(writeMessageInPv(text, pvId));
                }
                case "2" -> {
                    Logger.print(Commands.ENTER_YOUR_MESSAGE_ID);
                    String pvMessageId = scanner.nextLine();
                    if (isNumeric(pvMessageId)) {
                        Logger.printRed(deletePvMessage(Long.valueOf(pvMessageId), pvId));
                    } else {
                        Logger.printRed(Commands.INVALID_MESSAGE_ID);
                    }
                }
                case "3" -> {
                    Logger.print(Commands.ENTER_YOUR_MESSAGE_ID);
                    String messageId = scanner.nextLine();
                    if (isNumeric(messageId)) {
                        Logger.printRed(editPvMessage(Long.valueOf(messageId), pvId));
                    } else {
                        Logger.printRed(Commands.INVALID_MESSAGE_ID);
                    }
                }
                case "4" -> {
                    Logger.print(Commands.ENTER_YOUR_MESSAGE_ID);
                    String sourceMessageId = scanner.nextLine();
                    if (isNumeric(sourceMessageId)) {
                        PvMessageModel pvMessage = getPvMessage(Long.valueOf(sourceMessageId));
                        if (pvMessage != null && pvMessage.getPvId().equals(pvId)) {
                            forward(pvMessage);
                        } else {
                            Logger.printRed(Commands.INVALID_MESSAGE_ID);
                        }
                    } else {
                        Logger.printRed(Commands.INVALID_MESSAGE_ID);
                    }
                }
                case "5" -> {
                    flag = false;
                    pvNotifications.dispose();
                }
                default -> Logger.printRed(Commands.INVALID_COMMAND);
            }
        }
    }

    private Disposable getPvNotificationsInChat(Long pvId) {
        Flux<PvMessageModel> flux = client.get()
                .uri("pvNotifications" + SLASH + onlineUser.getUsername())
                .retrieve()
                .bodyToFlux(PvMessageModel.class);
        return flux.subscribe(pvMessageModel -> {
            if (pvMessageModel.getPvId().equals(pvId)) {
                printMessageInChat(pvMessageModel);
            } else {
                Logger.printGreen("a new message from " + pvMessageModel.getSender() + " received!");
            }
        });
    }

    private void groupChat(Long groupId) {
        Disposable groupNotifications = getGroupNotificationsInChat(groupId);
        boolean flag = true;
        while (flag) {
            GroupModel group = getGroup(groupId);
            if (group == null) {
                Logger.printRed(Commands.PLEASE_TRY_AGAIN);
                break;
            }
            group.getGroupMessages().forEach(this::printMessageInChat);
            Logger.print("1. send message\n2. delete message\n3. edit message\n4. forward message\n5. back");
            String option = scanner.nextLine();
            switch (option) {
                case "1" -> {
                    Logger.print("write your message:");
                    String text = scanner.nextLine();
                    Logger.printRed(writeMessageInGroup(text, groupId));
                }
                case "2" -> {
                    Logger.print(Commands.ENTER_YOUR_MESSAGE_ID);
                    String groupMessageId = scanner.nextLine();
                    if (isNumeric(groupMessageId)) {
                        Logger.printRed(deleteGroupMessage(Long.valueOf(groupMessageId), groupId));
                    } else {
                        Logger.printRed(Commands.INVALID_MESSAGE_ID);
                    }
                }
                case "3" -> {
                    Logger.print(Commands.ENTER_YOUR_MESSAGE_ID);
                    String messageId = scanner.nextLine();
                    if (isNumeric(messageId)) {
                        Logger.printRed(editGroupMessage(Long.valueOf(messageId), groupId));
                    } else {
                        Logger.printRed(Commands.INVALID_MESSAGE_ID);
                    }
                }
                case "4" -> {
                    Logger.print(Commands.ENTER_YOUR_MESSAGE_ID);
                    String sourceMessageId = scanner.nextLine();
                    if (isNumeric(sourceMessageId)) {
                        GroupMessageModel groupMessage = getGroupMessage(Long.valueOf(sourceMessageId));
                        if (groupMessage != null && groupMessage.getGroupId().equals(groupId)) {
                            forward(groupMessage);
                        } else {
                            Logger.printRed(Commands.INVALID_MESSAGE_ID);
                        }
                    } else {
                        Logger.printRed(Commands.INVALID_MESSAGE_ID);
                    }
                }
                case "5" -> {
                    flag = false;
                    groupNotifications.dispose();
                }
                default -> Logger.printRed(Commands.INVALID_COMMAND);
            }
        }
    }

    private Disposable getGroupNotificationsInChat(Long groupId) {
        Flux<GroupMessageModel> flux = client.get()
                .uri("groupNotifications" + SLASH + onlineUser.getUsername())
                .retrieve()
                .bodyToFlux(GroupMessageModel.class);
        return flux.subscribe(groupMessageModel -> {
            if (groupMessageModel.getGroupId().equals(groupId)) {
                printMessageInChat(groupMessageModel);
            } else {
                Logger.printGreen("a new message from " + groupMessageModel.getSender() + " in group with id "
                        + groupMessageModel.getGroupId() + " received!");
            }
        });
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
        onlineUser = getUser(onlineUser.getUsername());
        if (onlineUser == null) {
            return -1L;
        }
        for (PvModel pv : onlineUser.getPvs()) {
            if (getPeer(pv).equals(username)) {
                return pv.getId();
            }
        }
        PvModel newPv = createNewPv(username);
        onlineUser = getUser(onlineUser.getUsername());
        return newPv != null ? newPv.getId() : -1L;
    }

    private PvModel createNewPv(String peerUsername) {
        PvModel pvModel = PvModel.builder().first(onlineUser.getUsername()).second(peerUsername).build();
        ResponseEntity<PvModel> response = client.post()
                .uri(UrlPaths.PVS_URL_PATH)
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

    private String writeMessageInPv(String text, Long pvId) {
        Logger.print("1. send\n2. reply");
        String option = scanner.nextLine();
        long repliedMessageId = 0;
        switch (option) {
            case "1" -> {
                return sendPvMessage(text, pvId, repliedMessageId, null);
            }
            case "2" -> {
                Logger.print(Commands.ENTER_YOUR_MESSAGE_ID);
                String input = scanner.nextLine();
                if (!isNumeric(input)) {
                    return Commands.INVALID_MESSAGE_ID;
                }
                repliedMessageId = Long.parseLong(input);
                return sendPvMessage(text, pvId, repliedMessageId, null);
            }
            default -> {
                return Commands.INVALID_COMMAND;
            }
        }
    }

    private String writeMessageInGroup(String text, Long groupId) {
        Logger.print("1. send\n2. reply");
        String option = scanner.nextLine();
        long repliedMessageId = 0;
        switch (option) {
            case "1" -> {
                return sendGroupMessage(text, groupId, repliedMessageId, null);
            }
            case "2" -> {
                Logger.print(Commands.ENTER_YOUR_MESSAGE_ID);
                String input = scanner.nextLine();
                if (!isNumeric(input)) {
                    return Commands.INVALID_MESSAGE_ID;
                }
                repliedMessageId = Long.parseLong(input);
                return sendGroupMessage(text, groupId, repliedMessageId, null);
            }
            default -> {
                return Commands.INVALID_COMMAND;
            }
        }
    }

    private String sendPvMessage(String text, Long pvId, Long repliedMessageId, String forwardedFrom) {
        PvMessageModel pvMessageModel = PvMessageModel.builder()
                .text(text)
                .sender(onlineUser.getUsername())
                .pvId(pvId)
                .repliedMessageId(repliedMessageId)
                .forwardedFrom(forwardedFrom)
                .build();
        String url = UrlPaths.PV_MESSAGES_URL_PATH;
        ResponseEntity<String> response = client.post()
                .uri(url)
                .bodyValue(pvMessageModel)
                .retrieve()
                .onStatus(status -> status != HttpStatus.OK, clientResponse -> Mono.empty())
                .toEntity(String.class)
                .block();
        return response != null ? response.getBody() : Commands.PLEASE_TRY_AGAIN;
    }

    private String sendGroupMessage(String text, Long groupId, Long repliedMessageId, String forwardedFrom) {
        GroupMessageModel groupMessageModel = GroupMessageModel.builder()
                .text(text)
                .sender(onlineUser.getUsername())
                .groupId(groupId)
                .repliedMessageId(repliedMessageId)
                .forwardedFrom(forwardedFrom)
                .build();
        String url = UrlPaths.GROUP_MESSAGES_URL_PATH;
        ResponseEntity<String> response = client.post()
                .uri(url)
                .bodyValue(groupMessageModel)
                .retrieve()
                .onStatus(status -> status != HttpStatus.OK, clientResponse -> Mono.empty())
                .toEntity(String.class)
                .block();
        return response != null ? response.getBody() : Commands.PLEASE_TRY_AGAIN;
    }

    private void createGroup() {
        Logger.print("enter name of your group:");
        String name = scanner.nextLine();
        List<String> members = new ArrayList<>();
        members.add(onlineUser.getUsername());
        boolean flag = true;
        while (flag) {
            Logger.print("1. add member\n2. create");
            String option = scanner.nextLine();
            switch (option) {
                case "1" -> {
                    Logger.print("enter the username:");
                    String username = scanner.nextLine();
                    UserModel userModel = getUser(username);
                    if (userModel == null || userModel.getUsername() == null) {
                        Logger.printRed(Commands.USERNAME_DOESNT_EXIST);
                    } else if (members.contains(username)) {
                        Logger.printRed("already joined!");
                    } else {
                        Logger.printRed(username + " successfully added.");
                        members.add(username);
                    }
                }
                case "2" -> flag = false;
                default -> Logger.printRed(Commands.INVALID_COMMAND);
            }
        }
        String url = UrlPaths.GROUPS_URL_PATH;
        GroupModel groupModel = GroupModel.builder().owner(onlineUser.getUsername()).members(members).name(name).build();
        ResponseEntity<GroupModel> response = client.post()
                .uri(url)
                .bodyValue(groupModel)
                .retrieve()
                .toEntity(GroupModel.class)
                .block();
        if (response != null) {
            if (response.getStatusCode().equals(HttpStatus.CREATED)) {
                Logger.printRed("the group successfully created!");
            } else {
                Logger.printRed("invalid group content!");
            }
        } else {
            Logger.printRed(Commands.PLEASE_TRY_AGAIN);
        }
    }

    private String deletePvMessage(Long pvMessageId, Long pvId) {
        PvMessageModel pvMessageModel = getPvMessage(pvMessageId);
        if (pvMessageModel == null || !pvMessageModel.getPvId().equals(pvId) || !pvMessageModel.getSender().equals(onlineUser.getUsername())) {
            return Commands.INVALID_MESSAGE_ID;
        }
        String url = UrlPaths.PV_MESSAGES_URL_PATH + SLASH + pvMessageId;
        ResponseEntity<String> response = client.delete()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, onlineUser.getToken())
                .retrieve()
                .onStatus(status -> status != HttpStatus.OK, clientResponse -> Mono.empty())
                .toEntity(String.class)
                .block();
        if (response == null) {
            return Commands.PLEASE_TRY_AGAIN;
        }
        return response.getBody();
    }

    private String deleteGroupMessage(Long groupMessageId, Long groupId) {
        GroupMessageModel groupMessageModel = getGroupMessage(groupMessageId);
        if (groupMessageModel == null || !groupMessageModel.getGroupId().equals(groupId) || !groupMessageModel.getSender().equals(onlineUser.getUsername())) {
            return Commands.INVALID_MESSAGE_ID;
        }
        String url = UrlPaths.GROUP_MESSAGES_URL_PATH + SLASH + groupMessageId;
        ResponseEntity<String> response = client.delete()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, onlineUser.getToken())
                .retrieve()
                .onStatus(status -> status != HttpStatus.OK, clientResponse -> Mono.empty())
                .toEntity(String.class)
                .block();
        if (response == null) {
            return Commands.PLEASE_TRY_AGAIN;
        }
        return response.getBody();
    }

    private String editPvMessage(Long pvMessageId, Long pvId) {
        PvMessageModel pvMessageModel = getPvMessage(pvMessageId);
        if (pvMessageModel == null || !pvMessageModel.getPvId().equals(pvId) || !pvMessageModel.getSender().equals(onlineUser.getUsername())) {
            return Commands.INVALID_MESSAGE_ID;
        }
        Logger.print("write your new message:");
        String newText = scanner.nextLine();
        PvMessageModel newPvMessageModel = pvMessageModel.toBuilder().text(newText).build();
        String url = UrlPaths.PV_MESSAGES_URL_PATH + SLASH + pvMessageId;
        ResponseEntity<String> response = client.put()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, onlineUser.getToken()).bodyValue(newPvMessageModel)
                .retrieve()
                .onStatus(status -> status != HttpStatus.OK, clientResponse -> Mono.empty())
                .toEntity(String.class)
                .block();
        if (response == null) {
            return Commands.PLEASE_TRY_AGAIN;
        }
        return response.getBody();
    }

    private String editGroupMessage(Long groupMessageId, Long groupId) {
        GroupMessageModel groupMessageModel = getGroupMessage(groupMessageId);
        if (groupMessageModel == null || !groupMessageModel.getGroupId().equals(groupId) || !groupMessageModel.getSender().equals(onlineUser.getUsername())) {
            return Commands.INVALID_MESSAGE_ID;
        }
        Logger.print("write your new message:");
        String newText = scanner.nextLine();
        GroupMessageModel newGroupMessageModel = groupMessageModel.toBuilder().text(newText).build();
        String url = UrlPaths.GROUP_MESSAGES_URL_PATH + SLASH + groupMessageId;
        ResponseEntity<String> response = client.put()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, onlineUser.getToken()).bodyValue(newGroupMessageModel)
                .retrieve()
                .onStatus(status -> status != HttpStatus.OK, clientResponse -> Mono.empty())
                .toEntity(String.class)
                .block();
        if (response == null) {
            return Commands.PLEASE_TRY_AGAIN;
        }
        return response.getBody();
    }

    private void forward(MessageModel sourceMessage) {
        Logger.print("1. select chat by id\n2. select pv by username");
        String option = scanner.nextLine();
        switch (option) {
            case "1" -> {
                Logger.print("1. Group\n2. Pv");
                String option2 = scanner.nextLine();
                switch (option2) {
                    case "1" -> {
                        Logger.print("enter your group id:");
                        String input = scanner.nextLine();
                        if (isNumeric(input) && isOldGroup(Long.valueOf(input))) {
                            Long destinationGroupId = Long.valueOf(input);
                            Logger.printRed(sendGroupMessage(sourceMessage.getText(), destinationGroupId,
                                    0L, sourceMessage.getSender()));
                        } else {
                            Logger.printRed(Commands.INVALID_CHAT_ID);
                        }
                    }
                    case "2" -> {
                        Logger.print("enter your pv id:");
                        String input = scanner.nextLine();
                        if (isNumeric(input) && isOldPv(Long.valueOf(input))) {
                            Long destinationPvId = Long.valueOf(input);
                            Logger.printRed(sendPvMessage(sourceMessage.getText(), destinationPvId,
                                    0L, sourceMessage.getSender()));
                        } else {
                            Logger.printRed(Commands.INVALID_CHAT_ID);
                        }
                    }
                    default -> Logger.printRed(Commands.INVALID_COMMAND);
                }
            }
            case "2" -> {
                Logger.print("enter the username:");
                String username = scanner.nextLine();
                UserModel peer = getUser(username);
                if (peer != null) {
                    if (!peer.getUsername().equals(onlineUser.getUsername())) {
                        Long destinationPvId = getPvId(username);
                        if (destinationPvId != -1L) {
                            Logger.printRed(sendPvMessage(sourceMessage.getText(), destinationPvId,
                                    0L, sourceMessage.getSender()));
                        } else {
                            Logger.printRed(Commands.PLEASE_TRY_AGAIN);
                        }
                    } else {
                        Logger.printRed(Commands.ITS_YOUR_USERNAME);
                    }
                } else {
                    Logger.printRed(Commands.USERNAME_DOESNT_EXIST);
                }
            }
            default -> Logger.printRed(Commands.INVALID_COMMAND);
        }
    }

    private PvMessageModel getPvMessage(Long pvMessageId) {
        String url = UrlPaths.PV_MESSAGES_URL_PATH + SLASH + pvMessageId;
        ResponseEntity<PvMessageModel> response = client.get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, onlineUser.getToken())
                .retrieve()
                .onStatus(status -> status != HttpStatus.OK, clientResponse -> Mono.empty())
                .toEntity(PvMessageModel.class)
                .block();
        if (response == null) {
            Logger.printRed(Commands.PLEASE_TRY_AGAIN);
            return null;
        }
        return response.getBody();
    }

    private GroupMessageModel getGroupMessage(Long groupMessageId) {
        String url = UrlPaths.GROUP_MESSAGES_URL_PATH + SLASH + groupMessageId;
        ResponseEntity<GroupMessageModel> response = client.get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, onlineUser.getToken())
                .retrieve()
                .onStatus(status -> status != HttpStatus.OK, clientResponse -> Mono.empty())
                .toEntity(GroupMessageModel.class)
                .block();
        if (response == null) {
            Logger.printRed(Commands.PLEASE_TRY_AGAIN);
            return null;
        }
        return response.getBody();
    }

    private UserModel getUser(String username) {
        String url = UrlPaths.USERS_URL_PATH + SLASH + username;
        ResponseEntity<UserModel> response = client.get()
                .uri(url)
                .retrieve()
                .onStatus(status -> status != HttpStatus.OK, clientResponse -> Mono.empty())
                .toEntity(UserModel.class)
                .block();
        if (response == null) {
            Logger.printRed(Commands.PLEASE_TRY_AGAIN);
            return null;
        }
        return response.getBody();
    }

    private PvModel getPv(Long pvId) {
        String url = UrlPaths.PVS_URL_PATH + SLASH + pvId;
        ResponseEntity<PvModel> response = client.get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, onlineUser.getToken())
                .retrieve()
                .onStatus(status -> status != HttpStatus.OK, clientResponse -> Mono.empty())
                .toEntity(PvModel.class)
                .block();
        if (response == null) {
            Logger.printRed(Commands.PLEASE_TRY_AGAIN);
            return null;
        }
        return response.getBody();
    }

    private GroupModel getGroup(Long groupId) {
        String url = UrlPaths.GROUPS_URL_PATH + SLASH + groupId;
        ResponseEntity<GroupModel> response = client.get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, onlineUser.getToken())
                .retrieve()
                .onStatus(status -> status != HttpStatus.OK, clientResponse -> Mono.empty())
                .toEntity(GroupModel.class)
                .block();
        if (response == null) {
            Logger.printRed(Commands.PLEASE_TRY_AGAIN);
            return null;
        }
        return response.getBody();
    }

}
