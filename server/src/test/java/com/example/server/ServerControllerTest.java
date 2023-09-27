package com.example.server;

import com.example.server.entity.Message;
import com.example.server.entity.Chat;
import com.example.server.entity.Pv;
import com.example.server.entity.Group;
import com.example.server.entity.User;
import com.example.server.repository.MessageRepository;
import com.example.server.repository.ChatRepository;
import com.example.server.repository.UserRepository;
import org.example.model.GroupModel;
import org.example.model.MessageModel;
import org.example.model.PvModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.ArrayList;
import java.util.List;

import static com.example.server.ApiAddresses.HOST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ServerControllerTest {

    @Value(value = "${local.server.port}")
    private int port;

    @Autowired
    private final WebTestClient client = WebTestClient.bindToServer().build();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ChatRepository chatRepository;

    @BeforeEach
    public void setUpTestDatabase() {
        clearDatabase();
        createTestDatabase();
    }

    private void clearDatabase() {
        messageRepository.deleteAll();
        userRepository.deleteAll();
        chatRepository.deleteAll();
    }

    private void createTestDatabase() {
        Chat pv1 = new Pv("ali", "reza");
        Chat pv2 = new Pv("ali", "javad");
        Chat pv3 = new Pv("reza", "javad");
        chatRepository.saveAll(List.of(pv1, pv2, pv3));
        List<Chat> chats = chatRepository.findAll();

        Message message1 = new Message(1L, "aaa", "ali", chats.get(0).getId());
        Message message2 = new Message(2L, "bbb", "ali", chats.get(1).getId());
        Message message3 = new Message(3L, "ccc", "reza", chats.get(2).getId());
        messageRepository.saveAll(List.of(message1, message2, message3));


        Chat aliRezaChat = chats.get(0);
        Chat aliJavadChat = chats.get(1);
        Chat rezaJavadChat = chats.get(2);
        chatRepository.saveAll(List.of(aliRezaChat, aliJavadChat, rezaJavadChat));

        User ali = new User("ali", ServerController.generateToken());
        User reza = new User("reza", ServerController.generateToken());
        User javad = new User("javad", ServerController.generateToken());
        User amir = new User("amir", ServerController.generateToken());
        userRepository.saveAll(List.of(ali, reza, javad, amir));
    }

    @Test
    void testSignUp_newUsername() {
        String URL = HOST + port + ApiAddresses.SIGN_UP_API_URL;
        String newUsername = "amin";
        client.post().uri(URL).bodyValue(newUsername).exchange().expectStatus().isOk();
        assertTrue(userRepository.existsById(newUsername));
    }

    @Test
    void testSignUp_duplicateUsername() {
        userRepository.save(new User("amin", ServerController.generateToken()));
        String url = HOST + port + ApiAddresses.SIGN_UP_API_URL;
        String duplicateUsername = "amin";
        client.post().uri(url).bodyValue(duplicateUsername).exchange().expectStatus().isBadRequest();
    }

    @Test
    void testSignIn_newUsername() {
        String newUsername = "alireza";
        String url = HOST + port + ApiAddresses.SIGN_IN_API_URL + newUsername;
        client.get().uri(url).exchange().expectStatus().isBadRequest();
    }

    @Test
    void testSignIn_duplicateUsername() {
        userRepository.save(new User("alireza", ServerController.generateToken()));
        String duplicateUsername = "alireza";
        String url = HOST + port + ApiAddresses.SIGN_IN_API_URL + duplicateUsername;
        client.get().uri(url).exchange().expectStatus().isOk();
    }

    @Test
    void testNewMessage_invalidSender() {
        List<Chat> chats = chatRepository.findAll();
        MessageModel messageModel = new MessageModel(4L, "hello javad!", "alireza", chats.get(1).getId());
        String url = HOST + port + ApiAddresses.NEW_MESSAGE_API_URL;
        client.post().uri(url).bodyValue(messageModel).exchange().expectStatus().isBadRequest().expectBody(String.class).consumeWith(result ->
                assertEquals(Commands.INVALID_MESSAGE_CONTENT, result.getResponseBody()));
    }

    @Test
    void testNewMessage_chatDoesntExists() {
        List<Chat> chats = chatRepository.findAll();
        MessageModel messageModel = new MessageModel(4L, "hello javad!", "ali", chats.get(2).getId() + 1);
        String url = HOST + port + ApiAddresses.NEW_MESSAGE_API_URL;
        client.post().uri(url).bodyValue(messageModel).exchange().expectStatus().isBadRequest().expectBody(String.class).consumeWith(result ->
                assertEquals(Commands.INVALID_MESSAGE_CONTENT, result.getResponseBody()));
    }

    @Test
    void testNewMessage_ok() {
        List<Chat> chats = chatRepository.findAll();
        List<Message> messages = messageRepository.findAll();
        MessageModel messageModel = new MessageModel(((long) messages.get(2).getId()) + 1, "hello javad!", "ali", chats.get(1).getId());
        String url = HOST + port + ApiAddresses.NEW_MESSAGE_API_URL;
        client.post().uri(url).bodyValue(messageModel).exchange().expectStatus().isOk();
        Message message = new Message(messages.get(2).getId() + 1, messageModel.getText(), messageModel.getSender(), messageModel.getChatId());
        assertThat(message).isIn(messageRepository.findAll());
    }

    @Test
    void testNewPv_invalidFirstUsername() {
        PvModel pvModel = new PvModel("alireza", "ali");
        String url = HOST + port + ApiAddresses.NEW_PV_API_URL;
        client.post().uri(url).bodyValue(pvModel).exchange().expectStatus().isBadRequest().expectBody(String.class).consumeWith(result ->
                assertEquals(Commands.USERNAME_DOESNT_EXIST, result.getResponseBody()));
    }

    @Test
    void testNewPv_invalidSecondUsername() {
        PvModel pvModel = new PvModel("ali", "alireza");
        String url = HOST + port + ApiAddresses.NEW_PV_API_URL;
        client.post().uri(url).bodyValue(pvModel).exchange().expectStatus().isBadRequest().expectBody(String.class).consumeWith(result ->
                assertEquals(Commands.USERNAME_DOESNT_EXIST, result.getResponseBody()));
    }

    @Test
    void testNewPv_duplicateChat() {
        PvModel pvModel = new PvModel("ali", "reza");
        String url = HOST + port + ApiAddresses.NEW_PV_API_URL;
        client.post().uri(url).bodyValue(pvModel).exchange().expectStatus().isBadRequest().expectBody(String.class).consumeWith(result ->
                assertEquals(Commands.CHAT_ALREADY_EXISTS, result.getResponseBody()));
    }

    @Test
    void testNewPv_ok() {
        PvModel pvModel = new PvModel(chatRepository.findAll().get(2).getId() + 1, "ali", "amir");
        String url = HOST + port + ApiAddresses.NEW_PV_API_URL;
        client.post().uri(url).bodyValue(pvModel).exchange().expectStatus().isOk();
        Pv pv = new Pv(pvModel.getId(), pvModel.getFirst(), pvModel.getSecond());
        assertThat(pv).isIn(chatRepository.findAll());
    }

    @Test
    void testNewGroup_invalidContent() {
        GroupModel groupModel = new GroupModel("ali", new ArrayList<>(List.of("ali", "reza", "alireza")), "groupName");
        String url = HOST + port + ApiAddresses.NEW_GROUP_API_URL;
        client.post().uri(url).bodyValue(groupModel).exchange().expectStatus().isBadRequest().expectBody(String.class).consumeWith(result ->
                assertEquals(Commands.INVALID_GROUP_CONTENT, result.getResponseBody()));
    }

    @Test
    void testNewGroup_ok() {
        GroupModel groupModel = new GroupModel(chatRepository.findAll().get(2).getId() + 1, "ali",
                new ArrayList<>(List.of("ali", "reza", "javad")), "groupName");
        String url = HOST + port + ApiAddresses.NEW_GROUP_API_URL;
        client.post().uri(url).bodyValue(groupModel).exchange().expectStatus().isOk();
        Group group = new Group(groupModel.getId(), groupModel.getOwner(), groupModel.getMembers(), groupModel.getName());
        assertThat(group).isIn(chatRepository.findAll());
    }

    @Test
    void testDeleteMessage_messageDoesntExists() {
        Message message = messageRepository.findAll().get(2);
        String url = HOST + port + ApiAddresses.DELETE_MESSAGE_API_URL + (message.getId() + 1);
        client.delete().uri(url).header(HttpHeaders.AUTHORIZATION, userRepository.findById(message.getSender()).get().getToken()).exchange().expectStatus().isBadRequest().expectBody(String.class).consumeWith(result ->
                assertEquals(Commands.MESSAGE_DOESNT_EXIST, result.getResponseBody()));
    }

    @Test
    void testDeleteMessage_ok() {
        Message message = messageRepository.findAll().get(2);
        String url = HOST + port + ApiAddresses.DELETE_MESSAGE_API_URL + message.getId();
        client.delete().uri(url).header(HttpHeaders.AUTHORIZATION, userRepository.findById(message.getSender()).get().getToken()).exchange().expectStatus().isOk();
        assertThat(messageRepository.findById((long) message.getId())).isEmpty();
    }

    @Test
    void testEditMessage_messageDoesntExists() {
        Message message = messageRepository.findAll().get(2);
        Message newMessage = new Message("edited text!", message.getSender(), message.getChatId());
        String url = HOST + port + ApiAddresses.EDIT_MESSAGE_API_URL + (message.getId() + 1);
        client.put().uri(url).header(HttpHeaders.AUTHORIZATION, userRepository.findById(message.getSender()).get().getToken())
                .bodyValue(newMessage).exchange().expectStatus().isBadRequest().expectBody(String.class).consumeWith(result ->
                        assertEquals(Commands.MESSAGE_DOESNT_EXIST, result.getResponseBody()));
    }

    @Test
    void testEditMessage_ok() {
        Message message = messageRepository.findAll().get(2);
        Message newMessage = new Message("edited text!", message.getSender(), message.getChatId());
        String url = HOST + port + ApiAddresses.EDIT_MESSAGE_API_URL + message.getId();
        client.put().uri(url).header(HttpHeaders.AUTHORIZATION, userRepository.findById(message.getSender()).get().getToken())
                .bodyValue(newMessage).exchange().expectStatus().isOk();
        assertEquals(newMessage.getText(), messageRepository.findById(message.getId()).get().getText());
    }

    @Test
    void testGetMessage_invalidId() {
        Message message = messageRepository.findAll().get(2);
        String url = HOST + port + ApiAddresses.GET_MESSAGE_API_URL + (message.getId() + 1);
        client.get().uri(url).header(HttpHeaders.AUTHORIZATION, userRepository.findById(message.getSender()).get().getToken()).exchange().expectStatus().isBadRequest();
    }

    @Test
    void testGetMessage_ok() {
        Message message = messageRepository.findAll().get(2);
        MessageModel expected = message.createMessageModel();
        String url = HOST + port + ApiAddresses.GET_MESSAGE_API_URL + message.getId();
        client.get().uri(url).header(HttpHeaders.AUTHORIZATION, userRepository.findById(message.getSender()).get().getToken())
                .exchange().expectStatus().isOk().expectBody(MessageModel.class).consumeWith(result ->
                        assertEquals(result.getResponseBody(), expected));
    }


}
