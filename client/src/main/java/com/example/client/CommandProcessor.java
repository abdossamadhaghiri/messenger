package com.example.client;

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
                    groupChat(Long.valueOf(input));
                } else {
                    System.out.println(Commands.INVALID_CHAT_ID);
                }
            }
            case "2" -> {
                System.out.println("enter your pv id:");
                String input = scanner.nextLine();
                if (isNumeric(input) && isOldPv(Long.valueOf(input))) {
                    pvChat(Long.valueOf(input));
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
                pvChat(pvId);
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

    private void pvChat(Long pvId) {
        boolean flag = true;
        while (flag) {
            PvModel pv = getPv(pvId);
            if (pv == null) {
                System.out.println(Commands.PLEASE_TRY_AGAIN);
                break;
            }
            pv.getPvMessages().forEach(this::printMessageInChat);
            System.out.println("1. send message\n2. delete message\n3. edit message\n4. forward message\n5. back");
            String option = scanner.nextLine();
            switch (option) {
                case "1" -> {
                    System.out.println("write your message:");
                    String text = scanner.nextLine();
                    System.out.println(writeMessageInPv(text, pvId));
                }
                case "2" -> {
                    System.out.println("enter your message id:");
                    String pvMessageId = scanner.nextLine();
                    if (isNumeric(pvMessageId)) {
                        System.out.println(deletePvMessage(Long.valueOf(pvMessageId), pvId));
                    } else {
                        System.out.println(Commands.INVALID_MESSAGE_ID);
                    }
                }
                case "3" -> {
                    System.out.println("enter your message id:");
                    String messageId = scanner.nextLine();
                    if (isNumeric(messageId)) {
                        System.out.println(editPvMessage(Long.valueOf(messageId), pvId));
                    } else {
                        System.out.println(Commands.INVALID_MESSAGE_ID);
                    }
                }
                case "4" -> {
                    System.out.println("enter your message id:");
                    String sourceMessageId = scanner.nextLine();
                    if (isNumeric(sourceMessageId)) {
                        PvMessageModel pvMessage = getPvMessage(Long.valueOf(sourceMessageId));
                        if (pvMessage != null && pvMessage.getPvId().equals(pvId)) {
                            forward(pvMessage);
                        } else {
                            System.out.println(Commands.INVALID_MESSAGE_ID);
                        }
                    } else {
                        System.out.println(Commands.INVALID_MESSAGE_ID);
                    }
                }
                case "5" -> flag = false;
                default -> System.out.println(Commands.INVALID_COMMAND);
            }
        }
    }

    private void groupChat(Long groupId) {
        boolean flag = true;
        while (flag) {
            GroupModel group = getGroup(groupId);
            if (group == null) {
                System.out.println(Commands.PLEASE_TRY_AGAIN);
                break;
            }
            group.getGroupMessages().forEach(this::printMessageInChat);
            System.out.println("1. send message\n2. delete message\n3. edit message\n4. forward message\n5. back");
            String option = scanner.nextLine();
            switch (option) {
                case "1" -> {
                    System.out.println("write your message:");
                    String text = scanner.nextLine();
                    System.out.println(writeMessageInGroup(text, groupId));
                }
                case "2" -> {
                    System.out.println("enter your message id:");
                    String pvMessageId = scanner.nextLine();
                    if (isNumeric(pvMessageId)) {
                        System.out.println(deleteGroupMessage(Long.valueOf(pvMessageId), groupId));
                    } else {
                        System.out.println(Commands.INVALID_MESSAGE_ID);
                    }
                }
                case "3" -> {
                    System.out.println("enter your message id:");
                    String messageId = scanner.nextLine();
                    if (isNumeric(messageId)) {
                        System.out.println(editGroupMessage(Long.valueOf(messageId), groupId));
                    } else {
                        System.out.println(Commands.INVALID_MESSAGE_ID);
                    }
                }
                case "4" -> {
                    System.out.println("enter your message id:");
                    String sourceMessageId = scanner.nextLine();
                    if (isNumeric(sourceMessageId)) {
                        GroupMessageModel groupMessage = getGroupMessage(Long.valueOf(sourceMessageId));
                        if (groupMessage != null && groupMessage.getGroupId().equals(groupId)) {
                            forward(groupMessage);
                        } else {
                            System.out.println(Commands.INVALID_MESSAGE_ID);
                        }
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
        onlineUser = getUser(onlineUser.getUsername());
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

    private String writeMessageInPv(String text, Long pvId) {
        System.out.println("1. send\n2. reply");
        String option = scanner.nextLine();
        long repliedMessageId = 0;
        switch (option) {
            case "1" -> {
                return sendPvMessage(text, pvId, repliedMessageId, null);
            }
            case "2" -> {
                System.out.println("enter your message id:");
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
        System.out.println("1. send\n2. reply");
        String option = scanner.nextLine();
        long repliedMessageId = 0;
        switch (option) {
            case "1" -> {
                return sendGroupMessage(text, groupId, repliedMessageId, null);
            }
            case "2" -> {
                System.out.println("enter your message id:");
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
        String url = apiAddresses.getPvMessagesApiUrl();
        ResponseEntity<String> response = client.post()
                .uri(url)
                .bodyValue(pvMessageModel)
                .retrieve()
                .onStatus(status -> status == HttpStatus.BAD_REQUEST, clientResponse -> Mono.empty())
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
        String url = apiAddresses.getGroupMessagesApiUrl();
        ResponseEntity<String> response = client.post()
                .uri(url)
                .bodyValue(groupMessageModel)
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

    private String deletePvMessage(Long pvMessageId, Long pvId) {
        PvMessageModel pvMessageModel = getPvMessage(pvMessageId);
        if (pvMessageModel == null || !pvMessageModel.getPvId().equals(pvId) || !pvMessageModel.getSender().equals(onlineUser.getUsername())) {
            return Commands.INVALID_MESSAGE_ID;
        }
        String url = apiAddresses.getPvMessagesApiUrl() + File.separator + pvMessageId;
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

    private String deleteGroupMessage(Long groupMessageId, Long groupId) {
        GroupMessageModel groupMessageModel = getGroupMessage(groupMessageId);
        if (groupMessageModel == null || !groupMessageModel.getGroupId().equals(groupId) || !groupMessageModel.getSender().equals(onlineUser.getUsername())) {
            return Commands.INVALID_MESSAGE_ID;
        }
        String url = apiAddresses.getGroupMessagesApiUrl() + File.separator + groupMessageId;
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

    private String editPvMessage(Long pvMessageId, Long pvId) {
        PvMessageModel pvMessageModel = getPvMessage(pvMessageId);
        if (pvMessageModel == null || !pvMessageModel.getPvId().equals(pvId) || !pvMessageModel.getSender().equals(onlineUser.getUsername())) {
            return Commands.INVALID_MESSAGE_ID;
        }
        System.out.println("write your new message:");
        String newText = scanner.nextLine();
        PvMessageModel newPvMessageModel = pvMessageModel.toBuilder().text(newText).build();
        String url = apiAddresses.getPvMessagesApiUrl() + File.separator + pvMessageId;
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
        System.out.println("write your new message:");
        String newText = scanner.nextLine();
        GroupMessageModel newGroupMessageModel = groupMessageModel.toBuilder().text(newText).build();
        String url = apiAddresses.getGroupMessagesApiUrl() + File.separator + groupMessageId;
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
                            System.out.println(sendGroupMessage(sourceMessage.getText(), destinationGroupId,
                                    0L, sourceMessage.getSender()));
                        } else {
                            System.out.println(Commands.INVALID_CHAT_ID);
                        }
                    }
                    case "2" -> {
                        System.out.println("enter your pv id:");
                        String input = scanner.nextLine();
                        if (isNumeric(input) && isOldPv(Long.valueOf(input))) {
                            Long destinationPvId = Long.valueOf(input);
                            System.out.println(sendPvMessage(sourceMessage.getText(), destinationPvId,
                                    0L, sourceMessage.getSender()));
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
                        System.out.println(sendPvMessage(sourceMessage.getText(), destinationPvId,
                                0L, sourceMessage.getSender()));
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

    private PvMessageModel getPvMessage(Long pvMessageId) {
        String url = apiAddresses.getPvMessagesApiUrl() + File.separator + pvMessageId;
        ResponseEntity<PvMessageModel> response = client.get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, onlineUser.getToken())
                .retrieve()
                .onStatus(status -> status != HttpStatus.OK, clientResponse -> Mono.empty())
                .toEntity(PvMessageModel.class)
                .block();
        if (response == null) {
            System.out.println(Commands.PLEASE_TRY_AGAIN);
            return null;
        }
        return response.getBody();
    }

    private GroupMessageModel getGroupMessage(Long groupMessageId) {
        String url = apiAddresses.getGroupMessagesApiUrl() + File.separator + groupMessageId;
        ResponseEntity<GroupMessageModel> response = client.get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, onlineUser.getToken())
                .retrieve()
                .onStatus(status -> status != HttpStatus.OK, clientResponse -> Mono.empty())
                .toEntity(GroupMessageModel.class)
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
