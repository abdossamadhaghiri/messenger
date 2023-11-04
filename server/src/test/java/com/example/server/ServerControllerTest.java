package com.example.server;

import com.example.server.entity.GroupMessage;
import com.example.server.entity.Pv;
import com.example.server.entity.Group;
import com.example.server.entity.PvMessage;
import com.example.server.entity.User;
import com.example.server.repository.GroupMessageRepository;
import com.example.server.repository.GroupRepository;
import com.example.server.repository.PvMessageRepository;
import com.example.server.repository.PvRepository;
import com.example.server.repository.UserRepository;
import org.example.UrlPaths;
import org.example.model.GroupMessageModel;
import org.example.model.GroupModel;
import org.example.model.PvMessageModel;
import org.example.model.PvModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.example.UrlPaths.SLASH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ServerControllerTest {

    @Autowired
    private final WebTestClient client = WebTestClient.bindToServer().build();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PvMessageRepository pvMessageRepository;

    @Autowired
    private GroupMessageRepository groupMessageRepository;

    @Autowired
    private PvRepository pvRepository;

    @Autowired
    private GroupRepository groupRepository;

    private List<Pv> pvs;
    private List<Group> groups;
    private List<PvMessage> pvMessages;
    private List<GroupMessage> groupMessages;

    @BeforeEach
    public void setUpTestDatabase() {
        clearDatabase();
        createTestDatabase();
        pvs = pvRepository.findAll();
        groups = groupRepository.findAll();
        pvMessages = pvMessageRepository.findAll();
        groupMessages = groupMessageRepository.findAll();
    }

    private void clearDatabase() {
        pvMessageRepository.deleteAll();
        groupMessageRepository.deleteAll();;
        userRepository.deleteAll();
        pvRepository.deleteAll();
        groupRepository.deleteAll();
    }

    private void createTestDatabase() {
        Pv pv1 = Pv.builder().first("ali").second("reza").build();
        Pv pv2 = Pv.builder().first("ali").second("javad").build();
        Pv pv3 = Pv.builder().first("reza").second("javad").build();
        pvRepository.saveAll(List.of(pv1, pv2, pv3));
        pvs = pvRepository.findAll();

        Group group1 = Group.builder().owner("ali").members(new ArrayList<>(List.of("ali", "reza"))).name("group1").build();
        Group group2 = Group.builder().owner("javad").members(new ArrayList<>(List.of("javad", "ali"))).name("group2").build();
        Group group3 = Group.builder().owner("reza").members(new ArrayList<>(List.of("reza", "javad"))).name("group3").build();
        groupRepository.saveAll(List.of(group1, group2, group3));
        groups = groupRepository.findAll();

        PvMessage pvMessage1 = PvMessage.builder().id(1L).text("aaa").sender("ali").pvId(pvs.get(0).getId()).build();
        PvMessage pvMessage2 = PvMessage.builder().id(2L).text("bbb").sender("ali").pvId(pvs.get(1).getId()).build();
        PvMessage pvMessage3 = PvMessage.builder().id(3L).text("ccc").sender("reza").pvId(pvs.get(2).getId()).build();

        GroupMessage groupMessage1 = GroupMessage.builder().text("gm1").sender("ali").groupId(groups.get(0).getId()).build();
        GroupMessage groupMessage2 = GroupMessage.builder().text("gm2").sender("javad").groupId(groups.get(1).getId()).build();
        GroupMessage groupMessage3 = GroupMessage.builder().text("gm3").sender("reza").groupId(groups.get(2).getId()).build();

        Group aliRezaGroup = groups.get(0);
        Group javadAliGroup = groups.get(1);
        Group rezaJavadGroup = groups.get(2);
        aliRezaGroup.getGroupMessages().add(groupMessage1);
        rezaJavadGroup.getGroupMessages().add(groupMessage2);
        javadAliGroup.getGroupMessages().add(groupMessage3);
        groupRepository.saveAll(List.of(aliRezaGroup, rezaJavadGroup, javadAliGroup));

        Pv aliRezaPv = pvs.get(0);
        Pv aliJavadPv = pvs.get(1);
        Pv rezaJavadPv = pvs.get(2);
        aliRezaPv.getPvMessages().add(pvMessage1);
        aliJavadPv.getPvMessages().add(pvMessage2);
        rezaJavadPv.getPvMessages().add(pvMessage3);
        pvRepository.saveAll(List.of(aliRezaPv, aliJavadPv, rezaJavadPv));

        User ali = new User("ali", ServerController.generateToken(), new ArrayList<>(List.of(aliRezaPv, aliJavadPv)), new ArrayList<>(List.of(aliRezaGroup, javadAliGroup)));
        User reza = new User("reza", ServerController.generateToken(), new ArrayList<>(List.of(aliRezaPv, rezaJavadPv)), new ArrayList<>(List.of(aliRezaGroup, rezaJavadGroup)));
        User javad = new User("javad", ServerController.generateToken(), new ArrayList<>(List.of(aliJavadPv, rezaJavadPv)), new ArrayList<>(List.of(rezaJavadGroup, javadAliGroup)));
        User amir = new User("amir", ServerController.generateToken(), new ArrayList<>(), new ArrayList<>());
        userRepository.saveAll(List.of(ali, reza, javad, amir));
    }

    @Test
    void signUp_newUsername() {
        String URL = UrlPaths.SIGN_UP_URL_PATH;
        String newUsername = "amin";
        client.post().uri(URL).bodyValue(newUsername).exchange().expectStatus().isOk();
        assertTrue(userRepository.existsById(newUsername));
    }

    @Test
    void signUp_duplicateUsername() {
        userRepository.save(new User("amin", ServerController.generateToken(), new ArrayList<>(), new ArrayList<>()));
        String url = UrlPaths.SIGN_UP_URL_PATH;
        String duplicateUsername = "amin";
        client.post().uri(url).bodyValue(duplicateUsername).exchange().expectStatus().is4xxClientError();
    }

    @Test
    void signIn_newUsername() {
        String newUsername = "alireza";
        String url = UrlPaths.SIGN_IN_URL_PATH + SLASH + newUsername;
        client.get().uri(url).exchange().expectStatus().isNotFound();
    }

    @Test
    void signIn_duplicateUsername() {
        userRepository.save(new User("alireza", ServerController.generateToken(), new ArrayList<>(), new ArrayList<>()));
        String duplicateUsername = "alireza";
        String url = UrlPaths.SIGN_IN_URL_PATH + SLASH + duplicateUsername;
        client.get().uri(url).exchange().expectStatus().isOk();
    }

    @Test
    void getUser_newUsername() {
        String newUsername = "alireza";
        String url = UrlPaths.USERS_URL_PATH + SLASH + newUsername;
        client.get().uri(url).exchange().expectStatus().isNotFound();
    }

    @Test
    void getUser_duplicateUsername() {
        userRepository.save(new User("alireza", ServerController.generateToken(), new ArrayList<>(), new ArrayList<>()));
        String duplicateUsername = "alireza";
        String url = UrlPaths.USERS_URL_PATH + SLASH + duplicateUsername;
        client.get().uri(url).exchange().expectStatus().isOk();
    }

    @Test
    void newPvMessage_invalidSender() {
        PvMessageModel pvMessageModel = PvMessageModel.builder()
                .text("hello javad!")
                .sender("alireza")
                .pvId(pvs.get(1).getId())
                .build();
        client.post()
                .uri(UrlPaths.PV_MESSAGES_URL_PATH)
                .bodyValue(pvMessageModel)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(String.class)
                .isEqualTo(Commands.USERNAME_DOESNT_EXIST);
    }

    @Test
    void newPvMessage_chatDoesntExists() {
        PvMessageModel messageModel = PvMessageModel.builder()
                .text("hello javad!")
                .sender("ali")
                .pvId(pvs.get(2).getId() + 1)
                .build();
        client.post()
                .uri(UrlPaths.PV_MESSAGES_URL_PATH)
                .bodyValue(messageModel)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(String.class)
                .isEqualTo(Commands.CHAT_DOESNT_EXIST);
    }

    @Test
    void newPvMessage_chatIsNotInSendersChat() {
        PvMessageModel messageModel = PvMessageModel.builder()
                .text("hello javad!")
                .sender("ali")
                .pvId(pvs.get(2).getId())
                .build();
        client.post()
                .uri(UrlPaths.PV_MESSAGES_URL_PATH)
                .bodyValue(messageModel)
                .exchange()
                .expectStatus()
                .isForbidden()
                .expectBody(String.class)
                .isEqualTo(Commands.CHAT_IS_NOT_YOUR_CHAT);
    }

    @Test
    void newPvMessage_repliedMessageDoesntExists() {
        PvMessageModel messageModel = PvMessageModel.builder()
                .text("hello javad!")
                .sender("ali")
                .pvId(pvs.get(1).getId())
                .repliedMessageId(pvMessages.get(2).getId() + 1)
                .build();
        client.post()
                .uri(UrlPaths.PV_MESSAGES_URL_PATH)
                .bodyValue(messageModel)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(String.class)
                .isEqualTo(Commands.REPLIED_MESSAGE_DOESNT_EXIST);
    }

    @Test
    void newPvMessage_repliedMessageIsNotInThisChat() {
        PvMessageModel messageModel = PvMessageModel.builder()
                .text("hello javad!")
                .sender("ali")
                .pvId(pvs.get(1).getId())
                .repliedMessageId(pvMessages.get(2).getId())
                .build();
        client.post()
                .uri(UrlPaths.PV_MESSAGES_URL_PATH)
                .bodyValue(messageModel)
                .exchange()
                .expectStatus()
                .isForbidden()
                .expectBody(String.class)
                .isEqualTo(Commands.REPLIED_MESSAGE_IS_NOT_IN_THIS_CHAT);
    }

    @Test
    void newPvMessage_forwardedFromUsernameDoesntExist() {
        PvMessageModel messageModel = PvMessageModel.builder()
                .text("hello javad!")
                .sender("ali")
                .pvId(pvs.get(1).getId())
                .forwardedFrom("alireza")
                .build();
        client.post()
                .uri(UrlPaths.PV_MESSAGES_URL_PATH)
                .bodyValue(messageModel)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(String.class)
                .isEqualTo(Commands.FORWARDED_FROM_USERNAME_DOESNT_EXIST);
    }

    @Test
    void newPvMessage_ok_notReply_notForward() {
        PvMessageModel messageModel = PvMessageModel.builder()
                .id(pvMessages.get(2).getId() + 1)
                .text("hello javad!")
                .sender("ali")
                .pvId(pvs.get(1).getId())
                .build();
        String url = UrlPaths.PV_MESSAGES_URL_PATH;
        client.post().uri(url).bodyValue(messageModel).exchange().expectStatus().isOk();
        PvMessage pvMessage = PvMessage.fromPvMessageModel(messageModel);
        assertThat(pvMessage).isIn(pvMessageRepository.findAll()).isIn(pvRepository.findById(pvMessage.getPvId()).get().getPvMessages());
    }

    @Test
    void newPvMessage_ok_reply_notForward() {
        PvMessageModel messageModel = PvMessageModel.builder()
                .id(pvMessages.get(2).getId() + 1)
                .text("hello javad!")
                .sender("ali")
                .pvId(pvs.get(1).getId())
                .repliedMessageId(pvMessages.get(1).getId())
                .build();
        client.post().uri(UrlPaths.PV_MESSAGES_URL_PATH).bodyValue(messageModel).exchange().expectStatus().isOk();
        PvMessage pvMessage = PvMessage.fromPvMessageModel(messageModel);
        assertThat(pvMessage).isIn(pvMessageRepository.findAll()).isIn(pvRepository.findById(pvMessage.getPvId()).get().getPvMessages());
    }

    @Test
    void newPvMessage_ok_notReply_forward() {
        PvMessageModel messageModel = PvMessageModel.builder()
                .id(pvMessages.get(2).getId() + 1)
                .text("hello javad!")
                .sender("ali")
                .pvId(pvs.get(1).getId())
                .forwardedFrom("javad")
                .build();
        client.post().uri(UrlPaths.PV_MESSAGES_URL_PATH).bodyValue(messageModel).exchange().expectStatus().isOk();
        PvMessage pvMessage = PvMessage.fromPvMessageModel(messageModel);
        assertThat(pvMessage).isIn(pvMessageRepository.findAll()).isIn(pvRepository.findById(pvMessage.getPvId()).get().getPvMessages());
    }

    @Test
    void newPvMessage_ok_reply_forward() {
        PvMessageModel messageModel = PvMessageModel.builder()
                .id(pvMessages.get(2).getId() + 1)
                .text("hello javad!")
                .sender("ali")
                .pvId(pvs.get(1).getId())
                .repliedMessageId(pvMessages.get(1).getId())
                .forwardedFrom("javad")
                .build();
        client.post().uri(UrlPaths.PV_MESSAGES_URL_PATH).bodyValue(messageModel).exchange().expectStatus().isOk();
        PvMessage pvMessage = PvMessage.fromPvMessageModel(messageModel);
        assertThat(pvMessage).isIn(pvMessageRepository.findAll()).isIn(pvRepository.findById(pvMessage.getPvId()).get().getPvMessages());
    }

    @Test
    void newGroupMessage_invalidSender() {
        GroupMessageModel groupMessageModel = GroupMessageModel.builder()
                .text("hello javad!")
                .sender("alireza")
                .groupId(groups.get(1).getId())
                .build();
        client.post()
                .uri(UrlPaths.GROUP_MESSAGES_URL_PATH)
                .bodyValue(groupMessageModel)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(String.class)
                .isEqualTo(Commands.USERNAME_DOESNT_EXIST);
    }

    @Test
    void newGroupMessage_chatDoesntExists() {
        GroupMessageModel groupMessageModel = GroupMessageModel.builder()
                .text("hello javad!")
                .sender("ali")
                .groupId(groups.get(2).getId() + 1)
                .build();
        client.post()
                .uri(UrlPaths.GROUP_MESSAGES_URL_PATH)
                .bodyValue(groupMessageModel)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(String.class)
                .isEqualTo(Commands.CHAT_DOESNT_EXIST);
    }

    @Test
    void newGroupMessage_chatIsNotInSendersChat() {
        GroupMessageModel groupMessageModel = GroupMessageModel.builder()
                .text("hello javad!")
                .sender("ali")
                .groupId(groups.get(2).getId())
                .build();
        client.post()
                .uri(UrlPaths.GROUP_MESSAGES_URL_PATH)
                .bodyValue(groupMessageModel)
                .exchange()
                .expectStatus()
                .isForbidden()
                .expectBody(String.class)
                .isEqualTo(Commands.CHAT_IS_NOT_YOUR_CHAT);
    }

    @Test
    void newGroupMessage_repliedMessageDoesntExists() {
        GroupMessageModel groupMessageModel = GroupMessageModel.builder()
                .text("hello javad!")
                .sender("ali")
                .groupId(groups.get(1).getId())
                .repliedMessageId(groupMessages.get(2).getId() + 1)
                .build();
        client.post()
                .uri(UrlPaths.GROUP_MESSAGES_URL_PATH)
                .bodyValue(groupMessageModel)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(String.class)
                .isEqualTo(Commands.REPLIED_MESSAGE_DOESNT_EXIST);
    }

    @Test
    void newGroupMessage_repliedMessageIsNotInThisChat() {
        GroupMessageModel groupMessageModel = GroupMessageModel.builder()
                .text("hello javad!")
                .sender("ali")
                .groupId(groups.get(1).getId())
                .repliedMessageId(groupMessages.get(2).getId())
                .build();
        client.post()
                .uri(UrlPaths.GROUP_MESSAGES_URL_PATH)
                .bodyValue(groupMessageModel)
                .exchange()
                .expectStatus()
                .isForbidden()
                .expectBody(String.class)
                .isEqualTo(Commands.REPLIED_MESSAGE_IS_NOT_IN_THIS_CHAT);
    }

    @Test
    void newGroupMessage_forwardedFromUsernameDoesntExist() {
        GroupMessageModel groupMessageModel = GroupMessageModel.builder()
                .text("hello javad!")
                .sender("ali")
                .groupId(groups.get(1).getId())
                .forwardedFrom("alireza")
                .build();
        client.post()
                .uri(UrlPaths.GROUP_MESSAGES_URL_PATH)
                .bodyValue(groupMessageModel)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(String.class)
                .isEqualTo(Commands.FORWARDED_FROM_USERNAME_DOESNT_EXIST);
    }

    @Test
    void newGroupMessage_ok_notReply_notForward() {
        GroupMessageModel groupMessageModel = GroupMessageModel.builder()
                .id(groupMessages.get(2).getId() + 1)
                .text("hello javad!")
                .sender("ali")
                .groupId(groups.get(1).getId())
                .build();
        String url = UrlPaths.GROUP_MESSAGES_URL_PATH;
        client.post().uri(url).bodyValue(groupMessageModel).exchange().expectStatus().isOk();
        GroupMessage groupMessage = GroupMessage.fromGroupMessageModel(groupMessageModel);
        assertThat(groupMessage).isIn(groupMessageRepository.findAll()).isIn(groupRepository.findById(groupMessage.getGroupId()).get().getGroupMessages());
    }

    @Test
    void newGroupMessage_ok_reply_notForward() {
        GroupMessageModel groupMessageModel = GroupMessageModel.builder()
                .id(groupMessages.get(2).getId() + 1)
                .text("hello javad!")
                .sender("ali")
                .groupId(groups.get(1).getId())
                .repliedMessageId(groupMessages.get(1).getId())
                .build();
        client.post().uri(UrlPaths.GROUP_MESSAGES_URL_PATH).bodyValue(groupMessageModel).exchange().expectStatus().isOk();
        GroupMessage groupMessage = GroupMessage.fromGroupMessageModel(groupMessageModel);
        assertThat(groupMessage).isIn(groupMessageRepository.findAll()).isIn(groupRepository.findById(groupMessage.getGroupId()).get().getGroupMessages());
    }

    @Test
    void newGroupMessage_ok_notReply_forward() {
        GroupMessageModel groupMessageModel = GroupMessageModel.builder()
                .id(groupMessages.get(2).getId() + 1)
                .text("hello javad!")
                .sender("ali")
                .groupId(groups.get(1).getId())
                .forwardedFrom("javad")
                .build();
        client.post().uri(UrlPaths.GROUP_MESSAGES_URL_PATH).bodyValue(groupMessageModel).exchange().expectStatus().isOk();
        GroupMessage groupMessage = GroupMessage.fromGroupMessageModel(groupMessageModel);
        assertThat(groupMessage).isIn(groupMessageRepository.findAll()).isIn(groupRepository.findById(groupMessage.getGroupId()).get().getGroupMessages());
    }

    @Test
    void newGroupMessage_ok_reply_forward() {
        GroupMessageModel groupMessageModel = GroupMessageModel.builder()
                .id(groupMessages.get(2).getId() + 1)
                .text("hello javad!")
                .sender("ali")
                .groupId(groups.get(1).getId())
                .repliedMessageId(groupMessages.get(1).getId())
                .forwardedFrom("javad")
                .build();
        client.post().uri(UrlPaths.GROUP_MESSAGES_URL_PATH).bodyValue(groupMessageModel).exchange().expectStatus().isOk();
        GroupMessage groupMessage = GroupMessage.fromGroupMessageModel(groupMessageModel);
        assertThat(groupMessage).isIn(groupMessageRepository.findAll()).isIn(groupRepository.findById(groupMessage.getGroupId()).get().getGroupMessages());
    }

    @Test
    void testNewPv_invalidFirstUsername() {
        PvModel pvModel = PvModel.builder()
                .first("alireza")
                .second("ali")
                .build();
        String url = UrlPaths.PVS_URL_PATH;
        client.post().uri(url).bodyValue(pvModel).exchange().expectStatus().isNotFound();
    }

    @Test
    void testNewPv_invalidSecondUsername() {
        PvModel pvModel = PvModel.builder()
                .first("ali")
                .second("alireza")
                .build();
        String url = UrlPaths.PVS_URL_PATH;
        client.post().uri(url).bodyValue(pvModel).exchange().expectStatus().isNotFound();
    }

    @Test
    void testNewPv_duplicateChat() {
        PvModel pvModel = PvModel.builder()
                .first("ali")
                .second("reza")
                .build();
        String url = UrlPaths.PVS_URL_PATH;
        client.post().uri(url).bodyValue(pvModel).exchange().expectStatus().is4xxClientError();
    }

    @Test
    void testNewPv_ok() {
        PvModel pvModel = PvModel.builder()
                .id(pvs.get(2).getId() + 1)
                .first("ali")
                .second("amir")
                .build();
        String url = UrlPaths.PVS_URL_PATH;
        client.post().uri(url).bodyValue(pvModel).exchange().expectStatus().isCreated();
        Pv pv = Pv.fromPvModel(pvModel);
        assertThat(pv).isIn(pvRepository.findAll()).isIn(userRepository.findById(pv.getFirst()).get().getPvs())
                .isIn(userRepository.findById(pv.getSecond()).get().getPvs());
    }

    @Test
    void testNewGroup_invalidContent() {
        GroupModel groupModel = GroupModel.builder()
                .owner("ali")
                .members(new ArrayList<>(List.of("ali", "reza", "alireza")))
                .name("groupName")
                .build();
        String url = UrlPaths.GROUPS_URL_PATH;
        client.post().uri(url).bodyValue(groupModel).exchange().expectStatus().isNotFound();
    }

    @Test
    void testNewGroup_ok() {
        GroupModel groupModel = GroupModel.builder()
                .id(pvs.get(2).getId() + 1)
                .owner("ali")
                .members(new ArrayList<>(List.of("ali", "reza", "javad")))
                .name("groupName")
                .build();
        String url = UrlPaths.GROUPS_URL_PATH;
        client.post().uri(url).bodyValue(groupModel).exchange().expectStatus().isCreated();
        Group group = Group.fromGroupModel(groupModel);
        assertThat(group).isIn(groupRepository.findAll());
        group.getMembers().forEach(member -> assertThat(group).isIn(userRepository.findById(member).get().getGroups()));
    }

    @Test
    void deletePvMessage_invalidToken() {
        PvMessage pvMessage = pvMessages.get(2);
        String url = UrlPaths.PV_MESSAGES_URL_PATH + File.separator + pvMessage.getId();
        client.delete().uri(url).header(HttpHeaders.AUTHORIZATION, ServerController.generateToken()).exchange().expectStatus().isUnauthorized()
                .expectBody(String.class).consumeWith(result -> assertEquals(Commands.INVALID_TOKEN, result.getResponseBody()));
    }

    @Test
    void deletePvMessage_messageDoesntExists() {
        PvMessage pvMessage = pvMessages.get(2);
        User sender = userRepository.findById(pvMessage.getSender()).get();
        String url = UrlPaths.PV_MESSAGES_URL_PATH + File.separator + (pvMessage.getId() + 1);
        client.delete().uri(url).header(HttpHeaders.AUTHORIZATION, sender.getToken()).exchange().expectStatus().isNotFound()
                .expectBody(String.class).consumeWith(result -> assertEquals(Commands.MESSAGE_DOESNT_EXIST, result.getResponseBody()));
    }

    @Test
    void deletePvMessage_messageDoesntYourMessage() {
        PvMessage pvMessage = pvMessages.get(2);
        User user = userRepository.findById(pvMessages.get(1).getSender()).get();
        String url = UrlPaths.PV_MESSAGES_URL_PATH + File.separator + pvMessage.getId();
        client.delete().uri(url).header(HttpHeaders.AUTHORIZATION, user.getToken()).exchange().expectStatus().isForbidden()
                .expectBody(String.class).consumeWith(result -> assertEquals(Commands.MESSAGE_IS_NOT_YOUR_MESSAGE, result.getResponseBody()));
    }

    @Test
    void deletePvMessage_ok() {
        PvMessage pvMessage = pvMessages.get(2);
        User sender = userRepository.findById(pvMessage.getSender()).get();
        String url = UrlPaths.PV_MESSAGES_URL_PATH + File.separator + pvMessage.getId();
        client.delete().uri(url).header(HttpHeaders.AUTHORIZATION, sender.getToken()).exchange().expectStatus().isOk();
        assertThat(pvMessage).isNotIn(pvMessageRepository.findAll()).isNotIn(pvRepository.findById(pvMessage.getPvId()).get().getPvMessages());
    }

    @Test
    void deleteGroupMessage_invalidToken() {
        GroupMessage groupMessage = groupMessages.get(2);
        String url = UrlPaths.GROUP_MESSAGES_URL_PATH + File.separator + groupMessage.getId();
        client.delete().uri(url).header(HttpHeaders.AUTHORIZATION, ServerController.generateToken()).exchange().expectStatus().isUnauthorized()
                .expectBody(String.class).consumeWith(result -> assertEquals(Commands.INVALID_TOKEN, result.getResponseBody()));
    }

    @Test
    void deleteGroupMessage_messageDoesntExists() {
        GroupMessage groupMessage = groupMessages.get(2);
        User sender = userRepository.findById(groupMessage.getSender()).get();
        String url = UrlPaths.GROUP_MESSAGES_URL_PATH + File.separator + (groupMessage.getId() + 1);
        client.delete().uri(url).header(HttpHeaders.AUTHORIZATION, sender.getToken()).exchange().expectStatus().isNotFound()
                .expectBody(String.class).consumeWith(result -> assertEquals(Commands.MESSAGE_DOESNT_EXIST, result.getResponseBody()));
    }

    @Test
    void deleteGroupMessage_messageDoesntYourMessage() {
        GroupMessage groupMessage = groupMessages.get(2);
        User user = userRepository.findById(groupMessages.get(1).getSender()).get();
        String url = UrlPaths.GROUP_MESSAGES_URL_PATH + File.separator + groupMessage.getId();
        client.delete().uri(url).header(HttpHeaders.AUTHORIZATION, user.getToken()).exchange().expectStatus().isForbidden()
                .expectBody(String.class).consumeWith(result -> assertEquals(Commands.MESSAGE_IS_NOT_YOUR_MESSAGE, result.getResponseBody()));
    }

    @Test
    void deleteGroupMessage_ok() {
        GroupMessage groupMessage = groupMessages.get(2);
        User sender = userRepository.findById(groupMessage.getSender()).get();
        String url = UrlPaths.GROUP_MESSAGES_URL_PATH + File.separator + groupMessage.getId();
        client.delete().uri(url).header(HttpHeaders.AUTHORIZATION, sender.getToken()).exchange().expectStatus().isOk();
        assertThat(groupMessage).isNotIn(groupMessageRepository.findAll())
                .isNotIn(groupRepository.findById(groupMessage.getGroupId()).get().getGroupMessages());
    }

    @Test
    void editPvMessage_invalidToken() {
        PvMessage pvMessage = pvMessages.get(2);
        PvMessageModel pvMessageModel = PvMessageModel.builder()
                .id(pvMessage.getId())
                .text("edited text!")
                .sender(pvMessage.getSender())
                .pvId(pvMessage.getPvId())
                .build();
        String url = UrlPaths.PV_MESSAGES_URL_PATH + File.separator + pvMessage.getId();
        client.put()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, ServerController.generateToken())
                .bodyValue(pvMessageModel)
                .exchange()
                .expectStatus()
                .isUnauthorized()
                .expectBody(String.class)
                .isEqualTo(Commands.INVALID_TOKEN);
    }

    @Test
    void editPvMessage_messageDoesntExists() {
        PvMessage pvMessage = pvMessages.get(2);
        User sender = userRepository.findById(pvMessage.getSender()).get();
        PvMessageModel pvMessageModel = PvMessageModel.builder()
                .id(pvMessage.getId())
                .text("edited text!")
                .sender(pvMessage.getSender())
                .pvId(pvMessage.getPvId())
                .build();
        String url = UrlPaths.PV_MESSAGES_URL_PATH + File.separator + (pvMessage.getId() + 1);
        client.put()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, sender.getToken())
                .bodyValue(pvMessageModel)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(String.class)
                .isEqualTo(Commands.MESSAGE_DOESNT_EXIST);
    }

    @Test
    void editPvMessage_messageIsNotYourMessage() {
        PvMessage pvMessage = pvMessages.get(2);
        PvMessageModel pvMessageModel = PvMessageModel.builder()
                .id(pvMessage.getId())
                .text("edited text!")
                .sender(pvMessage.getSender())
                .pvId(pvMessage.getPvId())
                .build();
        User user = userRepository.findById(pvMessages.get(1).getSender()).get();
        String url = UrlPaths.PV_MESSAGES_URL_PATH + File.separator + pvMessage.getId();
        client.put()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, user.getToken())
                .bodyValue(pvMessageModel)
                .exchange()
                .expectStatus()
                .isForbidden()
                .expectBody(String.class)
                .isEqualTo(Commands.MESSAGE_IS_NOT_YOUR_MESSAGE);
    }

    @Test
    void editPvMessage_ok() {
        PvMessage pvMessage = pvMessages.get(2);
        PvMessageModel pvMessageModel = PvMessageModel.builder()
                .id(pvMessage.getId())
                .text("edited text!")
                .sender(pvMessage.getSender())
                .pvId(pvMessage.getPvId())
                .build();
        User sender = userRepository.findById(pvMessage.getSender()).get();
        String url = UrlPaths.PV_MESSAGES_URL_PATH + File.separator + pvMessage.getId();
        client.put()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, sender.getToken())
                .bodyValue(pvMessageModel)
                .exchange()
                .expectStatus()
                .isOk();
        assertEquals(pvMessageModel.getText(), pvMessageRepository.findById(pvMessage.getId()).get().getText());
    }

    @Test
    void editGroupMessage_invalidToken() {
        GroupMessage groupMessage = groupMessages.get(2);
        GroupMessageModel groupMessageModel = GroupMessageModel.builder()
                .id(groupMessage.getId())
                .text("edited text!")
                .sender(groupMessage.getSender())
                .groupId(groupMessage.getGroupId())
                .build();
        String url = UrlPaths.GROUP_MESSAGES_URL_PATH + File.separator + groupMessage.getId();
        client.put()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, ServerController.generateToken())
                .bodyValue(groupMessageModel)
                .exchange()
                .expectStatus()
                .isUnauthorized()
                .expectBody(String.class)
                .isEqualTo(Commands.INVALID_TOKEN);
    }

    @Test
    void editGroupMessage_messageDoesntExists() {
        GroupMessage groupMessage = groupMessages.get(2);
        User sender = userRepository.findById(groupMessage.getSender()).get();
        GroupMessageModel groupMessageModel = GroupMessageModel.builder()
                .id(groupMessage.getId())
                .text("edited text!")
                .sender(groupMessage.getSender())
                .groupId(groupMessage.getGroupId())
                .build();
        String url = UrlPaths.GROUP_MESSAGES_URL_PATH + File.separator + (groupMessage.getId() + 1);
        client.put()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, sender.getToken())
                .bodyValue(groupMessageModel)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(String.class)
                .isEqualTo(Commands.MESSAGE_DOESNT_EXIST);
    }

    @Test
    void editGroupMessage_messageIsNotYourMessage() {
        GroupMessage groupMessage = groupMessages.get(2);
        GroupMessageModel groupMessageModel = GroupMessageModel.builder()
                .id(groupMessage.getId())
                .text("edited text!")
                .sender(groupMessage.getSender())
                .groupId(groupMessage.getGroupId())
                .build();
        User user = userRepository.findById(groupMessages.get(1).getSender()).get();
        String url = UrlPaths.GROUP_MESSAGES_URL_PATH + File.separator + groupMessage.getId();
        client.put()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, user.getToken())
                .bodyValue(groupMessageModel)
                .exchange()
                .expectStatus()
                .isForbidden()
                .expectBody(String.class)
                .isEqualTo(Commands.MESSAGE_IS_NOT_YOUR_MESSAGE);
    }

    @Test
    void editGroupMessage_ok() {
        GroupMessage groupMessage = groupMessages.get(2);
        GroupMessageModel groupMessageModel = GroupMessageModel.builder()
                .id(groupMessage.getId())
                .text("edited text!")
                .sender(groupMessage.getSender())
                .groupId(groupMessage.getGroupId())
                .build();
        User sender = userRepository.findById(groupMessage.getSender()).get();
        String url = UrlPaths.GROUP_MESSAGES_URL_PATH + File.separator + groupMessage.getId();
        client.put()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, sender.getToken())
                .bodyValue(groupMessageModel)
                .exchange()
                .expectStatus()
                .isOk();
        assertEquals(groupMessageModel.getText(), groupMessageRepository.findById(groupMessage.getId()).get().getText());
    }

    @Test
    void getPvMessage_invalidToken() {
        PvMessage pvMessage = pvMessages.get(2);
        String url = UrlPaths.PV_MESSAGES_URL_PATH + File.separator + pvMessage.getId();
        client.get().uri(url).header(HttpHeaders.AUTHORIZATION, ServerController.generateToken()).exchange().expectStatus().isUnauthorized();
    }

    @Test
    void getPvMessage_messageDoesntExists() {
        PvMessage pvMessage = pvMessages.get(2);
        User sender = userRepository.findById(pvMessage.getSender()).get();
        String url = UrlPaths.PV_MESSAGES_URL_PATH + File.separator + (pvMessage.getId() + 1);
        client.get().uri(url).header(HttpHeaders.AUTHORIZATION, sender.getToken()).exchange().expectStatus().isNotFound();
    }

    @Test
    void getPvMessage_messageIsNotInYourChats() {
        PvMessage pvMessage = pvMessages.get(2);
        User user = userRepository.findById("ali").get();
        String url = UrlPaths.PV_MESSAGES_URL_PATH + File.separator + pvMessage.getId();
        client.get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, user.getToken())
                .exchange()
                .expectStatus()
                .isForbidden();
    }

    @Test
    void getPvMessage_ok() {
        PvMessage pvMessage = pvMessages.get(2);
        User user = userRepository.findById("javad").get();
        PvMessageModel expected = pvMessage.toPvMessageModel();
        String url = UrlPaths.PV_MESSAGES_URL_PATH + File.separator + pvMessage.getId();
        client.get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, user.getToken())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(PvMessageModel.class)
                .isEqualTo(expected);
    }

    @Test
    void getGroupMessage_invalidToken() {
        GroupMessage groupMessage = groupMessages.get(2);
        String url = UrlPaths.GROUP_MESSAGES_URL_PATH + File.separator + groupMessage.getId();
        client.get().uri(url).header(HttpHeaders.AUTHORIZATION, ServerController.generateToken()).exchange().expectStatus().isUnauthorized();
    }

    @Test
    void getGroupMessage_messageDoesntExists() {
        GroupMessage groupMessage = groupMessages.get(2);
        User sender = userRepository.findById(groupMessage.getSender()).get();
        String url = UrlPaths.GROUP_MESSAGES_URL_PATH + File.separator + (groupMessage.getId() + 1);
        client.get().uri(url).header(HttpHeaders.AUTHORIZATION, sender.getToken()).exchange().expectStatus().isNotFound();
    }

    @Test
    void getGroupMessage_messageIsNotInYourChats() {
        GroupMessage groupMessage = groupMessages.get(2);
        User user = userRepository.findById("ali").get();
        String url = UrlPaths.GROUP_MESSAGES_URL_PATH + File.separator + groupMessage.getId();
        client.get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, user.getToken())
                .exchange()
                .expectStatus()
                .isForbidden();
    }

    @Test
    void getGroupMessage_ok() {
        GroupMessage groupMessage = groupMessages.get(2);
        User user = userRepository.findById("javad").get();
        String url = UrlPaths.GROUP_MESSAGES_URL_PATH + File.separator + groupMessage.getId();
        client.get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, user.getToken())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(GroupMessageModel.class)
                .isEqualTo(groupMessage.toGroupMessageModel());
    }

    @Test
    void getPv_invalidToken() {
        client.get()
                .uri(UrlPaths.PVS_URL_PATH + File.separator + pvs.get(0).getId())
                .header(HttpHeaders.AUTHORIZATION, ServerController.generateToken())
                .exchange()
                .expectStatus()
                .isBadRequest();
    }

    @Test
    void getPv_chatDoesntExist() {
        client.get()
                .uri(UrlPaths.PVS_URL_PATH + File.separator + (pvs.get(2).getId() + 1))
                .header(HttpHeaders.AUTHORIZATION, userRepository.findById("ali").get().getToken())
                .exchange()
                .expectStatus()
                .isBadRequest();
    }

    @Test
    void getPv_chatIsNotInYourChats() {
        client.get()
                .uri(UrlPaths.PVS_URL_PATH + File.separator + pvs.get(0).getId())
                .header(HttpHeaders.AUTHORIZATION, userRepository.findById("javad").get().getToken())
                .exchange()
                .expectStatus()
                .isBadRequest();
    }

    @Test
    void getPv_ok() {
        client.get()
                .uri(UrlPaths.PVS_URL_PATH + File.separator + pvs.get(0).getId())
                .header(HttpHeaders.AUTHORIZATION, userRepository.findById("ali").get().getToken())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(PvModel.class)
                .isEqualTo(pvs.get(0).toPvModel());
    }

    @Test
    void getGroup_invalidToken() {
        client.get()
                .uri(UrlPaths.GROUPS_URL_PATH + File.separator + pvs.get(0).getId())
                .header(HttpHeaders.AUTHORIZATION, ServerController.generateToken())
                .exchange()
                .expectStatus()
                .isBadRequest();
    }

    @Test
    void getGroup_chatDoesntExist() {
        client.get()
                .uri(UrlPaths.GROUPS_URL_PATH + File.separator + (pvs.get(2).getId() + 1))
                .header(HttpHeaders.AUTHORIZATION, userRepository.findById("ali").get().getToken())
                .exchange()
                .expectStatus()
                .isBadRequest();
    }

    @Test
    void getGroup_chatIsNotInYourChats() {
        client.get()
                .uri(UrlPaths.GROUPS_URL_PATH + File.separator + pvs.get(0).getId())
                .header(HttpHeaders.AUTHORIZATION, userRepository.findById("javad").get().getToken())
                .exchange()
                .expectStatus()
                .isBadRequest();
    }

    @Test
    void getGroup_ok() {
        client.get()
                .uri(UrlPaths.GROUPS_URL_PATH + File.separator + pvs.get(0).getId())
                .header(HttpHeaders.AUTHORIZATION, userRepository.findById("ali").get().getToken())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(GroupModel.class)
                .isEqualTo(groups.get(0).toGroupModel());
    }

}