package com.example.server;

import com.example.server.repository.MessageRepository;
import com.example.server.repository.UserRepository;
import org.example.entity.Message;
import org.example.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
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

    @BeforeEach
    public void clearDatabase() {
        messageRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void testSignUp_newUsername() {
        List<User> expected = new ArrayList<>(List.of(new User("amin")));

        String URL = "http://localhost:" + port + "/signUp";
        String newUsername = "amin";
        client.post().uri(URL).bodyValue(newUsername).exchange().expectStatus().isOk();
        List<User> actual = userRepository.findAll();

        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
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
    void testGetUsers() {
        List<String> expected = List.of("ahmad", "ali", "javad", "mohammad", "reza");

        userRepository.saveAll(List.of(new User("ali"), new User("reza"), new User("mohammad"),
                new User("ahmad"), new User("javad")));
        String URL = "http://localhost:" + port + "/users";

        client.get().uri(URL).exchange().expectStatus().isOk().expectBody(new ParameterizedTypeReference<List<String>>() {
        }).consumeWith(result -> assertEquals(result.getResponseBody(), expected));
    }

    @Test
    void testGetMessagesInChatTest_IncorrectSender() {
        createTestDatabase();
        String URL = "http://localhost:" + port + "/messages/alireza/mohammad";
        client.get().uri(URL).exchange().expectStatus().isBadRequest();
    }

    @Test
    void testGetMessagesInChatTest_IncorrectReceiver() {
        createTestDatabase();
        String URL = "http://localhost:" + port + "/messages/ali/alireza";
        client.get().uri(URL).exchange().expectStatus().isBadRequest();
    }

    @Test
    void testGetMessagesInChatTest_CorrectSenderAndReceiver() {
        List<Message> expected = List.of(new Message(2, "bbbb", "ali", "mohammad"),
                new Message(7, "gggg", "mohammad", "ali"));
        createTestDatabase();
        String URL = "http://localhost:" + port + "/messages/mohammad/ali";
        client.get().uri(URL).exchange().expectStatus().isOk().expectBody(new ParameterizedTypeReference<List<Message>>() {
        }).consumeWith(result -> assertThat(result.getResponseBody()).usingRecursiveComparison().isEqualTo(expected));
    }

    @Test
    void testNewMessage() {
        List<Message> expected = new ArrayList<>(List.of(new Message(15, "test text", "ali", "reza")));

        String URL = "http://localhost:" + port + "/messages";
        Message newMessage = new Message(1, "test text", "ali", "reza");
        client.post().uri(URL).bodyValue(newMessage).exchange().expectStatus().isOk();
        List<Message> actual = messageRepository.findAll();

        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);

    }

    private void createTestDatabase() {
        userRepository.saveAll(List.of(new User("ali"), new User("reza"), new User("mohammad"),
                new User("ahmad"), new User("javad")));
        messageRepository.saveAll(new ArrayList<>(List.of(new Message(1, "aaaa", "ali", "reza"),
                new Message(2, "bbbb", "ali", "mohammad"), new Message(3, "cccc", "ahmad", "ali"),
                new Message(4, "dddd", "reza", "ali"), new Message(5, "eeee", "mohammad", "javad"),
                new Message(6, "ffff", "reza", "ali"), new Message(7, "gggg", "mohammad", "ali"))));
    }
}
