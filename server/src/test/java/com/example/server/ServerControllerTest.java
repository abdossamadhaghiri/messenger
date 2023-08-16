package com.example.server;

import com.example.server.Repository.MessageRepository;
import com.example.server.Repository.UserRepository;
import org.example.Entity.Message;
import org.example.Entity.User;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ServerControllerTest {

    @Value(value = "${local.server.port}")
    private int port;

    @Autowired
    private final WebTestClient client = WebTestClient.bindToServer().build();

    @MockBean
    private UserRepository userRepository;

    @Autowired
    private MessageRepository messageRepository;


    @Test
    public void getUsersTest() {
        List<String> expected = List.of("ahmad", "ali", "javad", "mohammad", "reza");

        userRepository.saveAll(List.of(new User("ali"), new User("reza"), new User("mohammad"),
                new User("ahmad"), new User("javad")));
        String URL = "http://localhost:" + port + "/users";

        client.get().uri(URL).exchange().expectBodyList(String.class).consumeWith(result -> assertEquals(expected, result.getResponseBody()));
    }

    @Test
    public void getMessagesInChatTest() {
        List<Message> expected = List.of(new Message("bbbb", "ali", "mohammad"),
                new Message("gggg", "mohammad", "ali"));

        messageRepository.saveAll(List.of(new Message(1, "aaaa", "ali", "reza"),
                new Message(2, "bbbb", "ali", "mohammad"), new Message(3, "cccc", "ahmad", "ali"),
                new Message(4, "dddd", "reza", "ali"), new Message(5, "eeee", "mohammad", "javad"),
                new Message(6, "ffff", "reza", "ali"), new Message(7, "gggg", "mohammad", "ali")));
        String URL = "http://localhost:" + port + "/messages/mohammad/ali";

    }

    @Test
    public void newMessageTest() {
        List<Message> expected = new ArrayList<>(List.of(new Message(1, "test text", "ali", "reza")));

        String URL = "http://localhost:" + port + "/messages";
        Message newMessage = new Message(1, "test text", "ali", "reza");
    }

    @Test
    public void newUserTest() {
        List<User> expected = new ArrayList<>(List.of(new User("amin")));

        String URL = "http://localhost:" + port + "/signUp";
        User newUser = new User("amin");

        client.post().uri(URL).bodyValue(newUser).exchange().expectBodyList(User.class);

        Mockito.verify(userRepository, times(1)).save(newUser);

    }
}
