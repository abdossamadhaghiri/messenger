package com.example.server;

import com.example.server.entity.Message;
import com.example.server.entity.Pv;
import com.example.server.entity.Group;
import com.example.server.entity.User;
import com.example.server.repository.GroupRepository;
import com.example.server.repository.MessageRepository;
import com.example.server.repository.PvRepository;
import com.example.server.repository.UserRepository;
import org.example.UrlPaths;
import org.example.model.GroupModel;
import org.example.model.MessageModel;
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
    private MessageRepository messageRepository;

    @Autowired
    private PvRepository pvRepository;

    @Autowired
    private GroupRepository groupRepository;

    private List<Pv> pvs;
    private List<Group> groups;
    private List<Message> messages;

    @BeforeEach
    public void setUpTestDatabase() {
        clearDatabase();
        createTestDatabase();
        pvs = pvRepository.findAll();
        groups = groupRepository.findAll();
        messages = messageRepository.findAll();
    }

    private void clearDatabase() {
        messageRepository.deleteAll();
        userRepository.deleteAll();
        pvRepository.deleteAll();
        groupRepository.deleteAll();
    }

    private void createTestDatabase() {
        Pv pv1 = new Pv("ali", "reza");
        Pv pv2 = new Pv("ali", "javad");
        Pv pv3 = new Pv("reza", "javad");
        pvRepository.saveAll(List.of(pv1, pv2, pv3));
        pvs = pvRepository.findAll();

        Message message1 = Message.builder().id(1L).text("aaa").sender("ali").chatId(pvs.get(0).getId()).build();
        Message message2 = Message.builder().id(2L).text("bbb").sender("ali").chatId(pvs.get(1).getId()).build();
        Message message3 = Message.builder().id(3L).text("ccc").sender("reza").chatId(pvs.get(2).getId()).build();

        Pv aliRezaChat = pvs.get(0);
        Pv aliJavadChat = pvs.get(1);
        Pv rezaJavadChat = pvs.get(2);
        aliRezaChat.getMessages().add(message1);
        aliJavadChat.getMessages().add(message2);
        rezaJavadChat.getMessages().add(message3);
        pvRepository.saveAll(List.of(aliRezaChat, aliJavadChat, rezaJavadChat));

        User ali = new User("ali", ServerController.generateToken(), new ArrayList<>(List.of(aliRezaChat, aliJavadChat)), new ArrayList<>());
        User reza = new User("reza", ServerController.generateToken(), new ArrayList<>(List.of(aliRezaChat, rezaJavadChat)), new ArrayList<>());
        User javad = new User("javad", ServerController.generateToken(), new ArrayList<>(List.of(aliJavadChat, rezaJavadChat)), new ArrayList<>());
        User amir = new User("amir", ServerController.generateToken(), new ArrayList<>(), new ArrayList<>());
        userRepository.saveAll(List.of(ali, reza, javad, amir));
    }

    @Test
    void newUser_newUsername() {
        String URL = UrlPaths.USERS_URL_PATH;
        String newUsername = "amin";
        client.post().uri(URL).bodyValue(newUsername).exchange().expectStatus().isOk();
        assertTrue(userRepository.existsById(newUsername));
    }

    @Test
    void newUser_duplicateUsername() {
        userRepository.save(new User("amin", ServerController.generateToken(), new ArrayList<>(), new ArrayList<>()));
        String url = UrlPaths.USERS_URL_PATH;
        String duplicateUsername = "amin";
        client.post().uri(url).bodyValue(duplicateUsername).exchange().expectStatus().isBadRequest();
    }

    @Test
    void getUser_newUsername() {
        String newUsername = "alireza";
        String url = UrlPaths.USERS_URL_PATH + "/" + newUsername;
        client.get().uri(url).exchange().expectStatus().isBadRequest();
    }

    @Test
    void getUser_duplicateUsername() {
        userRepository.save(new User("alireza", ServerController.generateToken(), new ArrayList<>(), new ArrayList<>()));
        String duplicateUsername = "alireza";
        String url = UrlPaths.USERS_URL_PATH + "/" + duplicateUsername;
        client.get().uri(url).exchange().expectStatus().isOk();
    }

    @Test
    void newMessage_invalidSender() {
        MessageModel messageModel = MessageModel.builder()
                .text("hello javad!")
                .sender("alireza")
                .chatId(pvs.get(1).getId())
                .build();
        client.post()
                .uri(UrlPaths.MESSAGES_URL_PATH)
                .bodyValue(messageModel)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(String.class)
                .isEqualTo(Commands.USERNAME_DOESNT_EXIST);
    }

    @Test
    void newMessage_chatDoesntExists() {
        MessageModel messageModel = MessageModel.builder()
                .text("hello javad!")
                .sender("ali")
                .chatId(pvs.get(2).getId() + 1)
                .build();
        client.post()
                .uri(UrlPaths.MESSAGES_URL_PATH)
                .bodyValue(messageModel)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(String.class)
                .isEqualTo(Commands.CHAT_DOESNT_EXIST);
    }

    @Test
    void newMessage_chatIsNotInSendersChat() {
        MessageModel messageModel = MessageModel.builder()
                .text("hello javad!")
                .sender("ali")
                .chatId(pvs.get(2).getId())
                .build();
        client.post()
                .uri(UrlPaths.MESSAGES_URL_PATH)
                .bodyValue(messageModel)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(String.class)
                .isEqualTo(Commands.CHAT_IS_NOT_YOUR_CHAT);
    }

    @Test
    void newMessage_repliedMessageDoesntExists() {
        MessageModel messageModel = MessageModel.builder()
                .text("hello javad!")
                .sender("ali")
                .chatId(pvs.get(1).getId())
                .repliedMessageId(messages.get(2).getId() + 1)
                .build();
        client.post()
                .uri(UrlPaths.MESSAGES_URL_PATH)
                .bodyValue(messageModel)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(String.class)
                .isEqualTo(Commands.REPLIED_MESSAGE_DOESNT_EXIST);
    }

    @Test
    void newMessage_repliedMessageIsNotInThisChat() {
        MessageModel messageModel = MessageModel.builder()
                .text("hello javad!")
                .sender("ali")
                .chatId(pvs.get(1).getId())
                .repliedMessageId(messages.get(2).getId())
                .build();
        client.post()
                .uri(UrlPaths.MESSAGES_URL_PATH)
                .bodyValue(messageModel)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(String.class)
                .isEqualTo(Commands.REPLIED_MESSAGE_IS_NOT_IN_THIS_CHAT);
    }

    @Test
    void newMessage_forwardedFromUsernameDoesntExist() {
        MessageModel messageModel = MessageModel.builder()
                .text("hello javad!")
                .sender("ali")
                .chatId(pvs.get(1).getId())
                .forwardedFrom("alireza")
                .build();
        client.post()
                .uri(UrlPaths.MESSAGES_URL_PATH)
                .bodyValue(messageModel)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(String.class)
                .isEqualTo(Commands.FORWARDED_FROM_USERNAME_DOESNT_EXIST);
    }

    @Test
    void newMessage_ok_notReply_notForward() {
        MessageModel messageModel = MessageModel.builder()
                .id(messages.get(2).getId() + 1)
                .text("hello javad!")
                .sender("ali")
                .chatId(pvs.get(1).getId())
                .build();
        String url = UrlPaths.MESSAGES_URL_PATH;
        client.post().uri(url).bodyValue(messageModel).exchange().expectStatus().isOk();
        Message message = Message.fromMessageModel(messageModel);
        assertThat(message).isIn(messageRepository.findAll()).isIn(pvRepository.findById(message.getChatId()).get().getMessages());
    }

    @Test
    void newMessage_ok_reply_notForward() {
        MessageModel messageModel = MessageModel.builder()
                .id(messages.get(2).getId() + 1)
                .text("hello javad!")
                .sender("ali")
                .chatId(pvs.get(1).getId())
                .repliedMessageId(messages.get(1).getId())
                .build();
        client.post().uri(UrlPaths.MESSAGES_URL_PATH).bodyValue(messageModel).exchange().expectStatus().isOk();
        Message message = Message.fromMessageModel(messageModel);
        assertThat(message).isIn(messageRepository.findAll()).isIn(pvRepository.findById(message.getChatId()).get().getMessages());
    }

    @Test
    void newMessage_ok_notReply_forward() {
        MessageModel messageModel = MessageModel.builder()
                .id(messages.get(2).getId() + 1)
                .text("hello javad!")
                .sender("ali")
                .chatId(pvs.get(1).getId())
                .forwardedFrom("javad")
                .build();
        client.post().uri(UrlPaths.MESSAGES_URL_PATH).bodyValue(messageModel).exchange().expectStatus().isOk();
        Message message = Message.fromMessageModel(messageModel);
        assertThat(message).isIn(messageRepository.findAll()).isIn(pvRepository.findById(message.getChatId()).get().getMessages());
    }

    @Test
    void newMessage_ok_reply_forward() {
        MessageModel messageModel = MessageModel.builder()
                .id(messages.get(2).getId() + 1)
                .text("hello javad!")
                .sender("ali")
                .chatId(pvs.get(1).getId())
                .repliedMessageId(messages.get(1).getId())
                .forwardedFrom("javad")
                .build();
        client.post().uri(UrlPaths.MESSAGES_URL_PATH).bodyValue(messageModel).exchange().expectStatus().isOk();
        Message message = Message.fromMessageModel(messageModel);
        assertThat(message).isIn(messageRepository.findAll()).isIn(pvRepository.findById(message.getChatId()).get().getMessages());
    }

    @Test
    void testNewPv_invalidFirstUsername() {
        PvModel pvModel = PvModel.builder()
                .first("alireza")
                .second("ali")
                .build();
        String url = UrlPaths.PVS_URL_PATH;
        client.post().uri(url).bodyValue(pvModel).exchange().expectStatus().isBadRequest();
    }

    @Test
    void testNewPv_invalidSecondUsername() {
        PvModel pvModel = PvModel.builder()
                .first("ali")
                .second("alireza")
                .build();
        String url = UrlPaths.PVS_URL_PATH;
        client.post().uri(url).bodyValue(pvModel).exchange().expectStatus().isBadRequest();
    }

    @Test
    void testNewPv_duplicateChat() {
        PvModel pvModel = PvModel.builder()
                .first("ali")
                .second("reza")
                .build();
        String url = UrlPaths.PVS_URL_PATH;
        client.post().uri(url).bodyValue(pvModel).exchange().expectStatus().isBadRequest();
    }

    @Test
    void testNewPv_ok() {
        PvModel pvModel = PvModel.builder()
                .id(pvs.get(2).getId() + 1)
                .first("ali")
                .second("amir")
                .messages(new ArrayList<>())
                .build();
        String url = UrlPaths.PVS_URL_PATH;
        client.post().uri(url).bodyValue(pvModel).exchange().expectStatus().isOk();
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
                .messages(new ArrayList<>())
                .build();
        String url = UrlPaths.GROUPS_URL_PATH;
        client.post().uri(url).bodyValue(groupModel).exchange().expectStatus().isBadRequest();
    }

    @Test
    void testNewGroup_ok() {
        GroupModel groupModel = GroupModel.builder()
                .id(pvs.get(2).getId() + 1)
                .owner("ali")
                .members(new ArrayList<>(List.of("ali", "reza", "javad")))
                .name("groupName")
                .messages(new ArrayList<>())
                .build();
        String url = UrlPaths.GROUPS_URL_PATH;
        client.post().uri(url).bodyValue(groupModel).exchange().expectStatus().isOk();
        Group group = Group.fromGroupModel(groupModel);
        assertThat(group).isIn(groupRepository.findAll());
        group.getMembers().forEach(member -> assertThat(group).isIn(userRepository.findById(member).get().getGroups()));
    }

    @Test
    void testDeleteMessage_invalidToken() {
        Message message = messages.get(2);
        String url = UrlPaths.MESSAGES_URL_PATH + "/" + message.getId();
        client.delete().uri(url).header(HttpHeaders.AUTHORIZATION, ServerController.generateToken()).exchange().expectStatus().isBadRequest()
                .expectBody(String.class).consumeWith(result -> assertEquals(Commands.INVALID_TOKEN, result.getResponseBody()));
    }

    @Test
    void testDeleteMessage_messageDoesntExists() {
        Message message = messages.get(2);
        User sender = userRepository.findById(message.getSender()).get();
        String url = UrlPaths.MESSAGES_URL_PATH + "/" + (message.getId() + 1);
        client.delete().uri(url).header(HttpHeaders.AUTHORIZATION, sender.getToken()).exchange().expectStatus().isBadRequest()
                .expectBody(String.class).consumeWith(result -> assertEquals(Commands.MESSAGE_DOESNT_EXIST, result.getResponseBody()));
    }

    @Test
    void testDeleteMessage_messageDoesntYourMessage() {
        Message message = messages.get(2);
        User user = userRepository.findById(messages.get(1).getSender()).get();
        String url = UrlPaths.MESSAGES_URL_PATH + "/" + message.getId();
        client.delete().uri(url).header(HttpHeaders.AUTHORIZATION, user.getToken()).exchange().expectStatus().isBadRequest()
                .expectBody(String.class).consumeWith(result -> assertEquals(Commands.MESSAGE_IS_NOT_YOUR_MESSAGE, result.getResponseBody()));
    }

    @Test
    void testDeleteMessage_ok() {
        Message message = messages.get(2);
        User sender = userRepository.findById(message.getSender()).get();
        String url = UrlPaths.MESSAGES_URL_PATH + "/" + message.getId();
        client.delete().uri(url).header(HttpHeaders.AUTHORIZATION, sender.getToken()).exchange().expectStatus().isOk();
        assertThat(message).isNotIn(messageRepository.findAll()).isNotIn(pvRepository.findById(message.getChatId()).get().getMessages());
    }

    @Test
    void editMessage_invalidToken() {
        Message message = messages.get(2);
        MessageModel messageModel = MessageModel.builder()
                                                .id(message.getId())
                                                .text("edited text!")
                                                .sender(message.getSender())
                                                .chatId(message.getChatId())
                                                .build();
        String url = UrlPaths.MESSAGES_URL_PATH + "/" + message.getId();
        client.put()
              .uri(url)
              .header(HttpHeaders.AUTHORIZATION, ServerController.generateToken())
              .bodyValue(messageModel)
              .exchange()
              .expectStatus()
              .isBadRequest()
              .expectBody(String.class)
              .isEqualTo(Commands.INVALID_TOKEN);
    }

    @Test
    void editMessage_messageDoesntExists() {
        Message message = messages.get(2);
        User sender = userRepository.findById(message.getSender()).get();
        MessageModel messageModel = MessageModel.builder()
                                                .id(message.getId())
                                                .text("edited text!")
                                                .sender(message.getSender())
                                                .chatId(message.getChatId())
                                                .build();
        String url = UrlPaths.MESSAGES_URL_PATH + "/" + (message.getId() + 1);
        client.put()
              .uri(url)
              .header(HttpHeaders.AUTHORIZATION, sender.getToken())
              .bodyValue(messageModel)
              .exchange()
              .expectStatus()
              .isBadRequest()
              .expectBody(String.class)
              .isEqualTo(Commands.MESSAGE_DOESNT_EXIST);
    }

    @Test
    void editMessage_messageDoesntYourMessage() {
        Message message = messages.get(2);
        MessageModel messageModel = MessageModel.builder()
                                                .id(message.getId())
                                                .text("edited text!")
                                                .sender(message.getSender())
                                                .chatId(message.getChatId())
                                                .build();
        User user = userRepository.findById(messages.get(1).getSender()).get();
        String url = UrlPaths.MESSAGES_URL_PATH + "/" + message.getId();
        client.put()
              .uri(url)
              .header(HttpHeaders.AUTHORIZATION, user.getToken())
              .bodyValue(messageModel)
              .exchange()
              .expectStatus()
              .isBadRequest()
              .expectBody(String.class)
              .isEqualTo(Commands.MESSAGE_IS_NOT_YOUR_MESSAGE);
    }

    @Test
    void editMessage_ok() {
        Message message = messages.get(2);
        MessageModel messageModel = MessageModel.builder()
                                                .id(message.getId())
                                                .text("edited text!")
                                                .sender(message.getSender())
                                                .chatId(message.getChatId())
                                                .build();
        User sender = userRepository.findById(message.getSender()).get();
        String url = UrlPaths.MESSAGES_URL_PATH + "/" + message.getId();
        client.put()
              .uri(url)
              .header(HttpHeaders.AUTHORIZATION, sender.getToken())
              .bodyValue(messageModel)
              .exchange()
              .expectStatus()
              .isOk();
        assertEquals(messageModel.getText(), messageRepository.findById(message.getId()).get().getText());
    }

    @Test
    void testGetMessage_invalidToken() {
        Message message = messages.get(2);
        String url = UrlPaths.MESSAGES_URL_PATH + "/" + message.getId();
        client.get().uri(url).header(HttpHeaders.AUTHORIZATION, ServerController.generateToken()).exchange().expectStatus().isBadRequest();
    }

    @Test
    void testGetMessage_messageDoesntExists() {
        Message message = messages.get(2);
        User sender = userRepository.findById(message.getSender()).get();
        String url = UrlPaths.MESSAGES_URL_PATH + "/" + (message.getId() + 1);
        client.get().uri(url).header(HttpHeaders.AUTHORIZATION, sender.getToken()).exchange().expectStatus().isBadRequest();
    }

    @Test
    void getMessage_messageIsNotInYourChats() {
        Message message = messages.get(2);
        User user = userRepository.findById("ali").get();
        String url = UrlPaths.MESSAGES_URL_PATH + "/" + message.getId();
        client.get()
              .uri(url)
              .header(HttpHeaders.AUTHORIZATION, user.getToken())
              .exchange()
              .expectStatus()
              .isBadRequest();
    }

    @Test
    void testGetMessage_ok() {
        Message message = messages.get(2);
        User user = userRepository.findById("javad").get();
        MessageModel expected = message.toMessageModel();
        String url = UrlPaths.MESSAGES_URL_PATH + "/" + message.getId();
        client.get()
              .uri(url)
              .header(HttpHeaders.AUTHORIZATION, user.getToken())
              .exchange()
              .expectStatus()
              .isOk()
              .expectBody(MessageModel.class)
              .isEqualTo(expected);
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
