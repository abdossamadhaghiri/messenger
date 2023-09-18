package com.example.server;

import com.example.server.repository.*;
import org.example.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;

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

        Message message1 = new Message(1, "aaa", "ali", chats.get(0).getId());
        Message message2 = new Message(2, "bbb", "ali", chats.get(1).getId());
        Message message3 = new Message(3, "ccc", "reza", chats.get(2).getId());
        messageRepository.saveAll(List.of(message1, message2, message3));


        Chat aliRezaChat = chats.get(0);
        Chat aliJavadChat = chats.get(1);
        Chat rezaJavadChat = chats.get(2);
        chatRepository.saveAll(List.of(aliRezaChat, aliJavadChat, rezaJavadChat));

        User ali = new User("ali");
        User reza = new User("reza");
        User javad = new User("javad");
        User amir = new User("amir");
        userRepository.saveAll(List.of(ali, reza, javad, amir));
    }

    @Test
    void testSignUp_newUsername() {
        User user = new User("amin");
        String URL = "http://localhost:" + port + "/signUp";
        String newUsername = "amin";
        client.post().uri(URL).bodyValue(newUsername).exchange().expectStatus().isOk();
        assertThat(user).usingRecursiveComparison().isIn(userRepository.findAll());
    }

    @Test
    void testSignUp_duplicateUsername() {
        userRepository.save(new User("amin"));
        String URL = "http://localhost:" + port + "/signUp";
        String duplicateUsername = "amin";
        client.post().uri(URL).bodyValue(duplicateUsername).exchange().expectStatus().isBadRequest();
    }

    @Test
    void testSignIn_newUsername() {
        String newUsername = "alireza";
        String URL = "http://localhost:" + port + "/signIn/" + newUsername;
        client.get().uri(URL).exchange().expectStatus().isBadRequest();
    }

    @Test
    void testSignIn_duplicateUsername() {
        userRepository.save(new User("alireza"));
        String duplicateUsername = "alireza";
        String URL = "http://localhost:" + port + "/signIn/" + duplicateUsername;
        client.get().uri(URL).exchange().expectStatus().isOk();
    }

    @Test
    void testNewMessage_invalidSender() {
        List<Chat> chats = chatRepository.findAll();
        Message message = new Message(4, "hello javad!", "alireza", chats.get(1).getId());
        String url = "http://localhost:" + port + "/messages";
        client.post().uri(url).bodyValue(message).exchange().expectStatus().isBadRequest().expectBody(String.class).consumeWith(result ->
                assertEquals("invalid message content!", result.getResponseBody()));
    }

    @Test
    void testNewMessage_chatDoesntExists() {
        List<Chat> chats = chatRepository.findAll();
        Message message = new Message(4, "hello javad!", "ali", chats.get(2).getId() + 1);
        String url = "http://localhost:" + port + "/messages";
        client.post().uri(url).bodyValue(message).exchange().expectStatus().isBadRequest().expectBody(String.class).consumeWith(result ->
                assertEquals("invalid message content!", result.getResponseBody()));
    }

    @Test
    void testNewMessage_ok() {
        List<Chat> chats = chatRepository.findAll();
        List<Message> messages = messageRepository.findAll();
        Message message = new Message(messages.get(2).getId() + 1, "hello javad!", "ali", chats.get(1).getId());
        String url = "http://localhost:" + port + "/messages";
        client.post().uri(url).bodyValue(message).exchange().expectStatus().isOk();
        assertThat(message).usingRecursiveComparison().isIn(messageRepository.findAll());
    }

    @Test
    void testNewPv_invalidFirstUsername() {
        Chat pv = new Pv("alireza", "ali");
        String url = "http://localhost:" + port + "/chat";
        client.post().uri(url).bodyValue(pv).exchange().expectStatus().isBadRequest().expectBody(String.class).consumeWith(result ->
                assertEquals("username doesnt exist.", result.getResponseBody()));
    }

    @Test
    void testNewPv_invalidSecondUsername() {
        Chat pv = new Pv("ali", "alireza");
        String url = "http://localhost:" + port + "/chat";
        client.post().uri(url).bodyValue(pv).exchange().expectStatus().isBadRequest().expectBody(String.class).consumeWith(result ->
                assertEquals("username doesnt exist.", result.getResponseBody()));
    }

    @Test
    void testNewPv_duplicateChat() {
        Chat pv = new Pv("ali", "reza");
        String url = "http://localhost:" + port + "/chat";
        client.post().uri(url).bodyValue(pv).exchange().expectStatus().isBadRequest().expectBody(String.class).consumeWith(result ->
                assertEquals("the chat already exists.", result.getResponseBody()));
    }

    @Test
    void testNewPv_ok() {
        Chat pv = new Pv(chatRepository.findAll().get(2).getId() + 1, "ali", "amir");
        String url = "http://localhost:" + port + "/chat";
        client.post().uri(url).bodyValue(pv).exchange().expectStatus().isOk();
        assertThat(pv).usingRecursiveComparison().isIn(chatRepository.findAll());
    }

    @Test
    void testNewGroup_invalidContent() {
        Group group = new Group("ali", new ArrayList<>(List.of("ali", "reza", "alireza")), "groupName");
        String url = "http://localhost:" + port + "/groups";
        client.post().uri(url).bodyValue(group).exchange().expectStatus().isBadRequest().expectBody(String.class).consumeWith(result ->
                assertEquals("invalid group content!", result.getResponseBody()));
    }

    @Test
    void testNewGroup_ok() {
        Group group = new Group(chatRepository.findAll().get(2).getId() + 1, "ali",
                new ArrayList<>(List.of("ali", "reza", "javad")), "groupName");
        String url = "http://localhost:" + port + "/groups";
        client.post().uri(url).bodyValue(group).exchange().expectStatus().isOk();
        assertThat(group).usingRecursiveComparison().isIn(chatRepository.findAll());
    }

}
