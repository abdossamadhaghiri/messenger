package com.example.server;

import com.example.server.Repository.MessageRepository;
import com.example.server.Repository.UserRepository;
import org.example.Entity.Message;
import org.example.Entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ServerControllerTest {

    @Value(value = "${local.server.port}")
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Test
    public void getUsersTest() {
        userRepository.saveAll(List.of(new User("ali"), new User("reza"), new User("mohammad"),
                new User("ahmad"), new User("javad")));
        List<String> actual = restTemplate.getForObject("http://localhost:" + port + "/users", List.class);

        List<String> expected = List.of("ahmad", "ali", "javad", "mohammad", "reza");

        assertEquals(expected, actual);
    }

    @Test
    public void getMessagesInChatTest() {
        messageRepository.saveAll(List.of(new Message(1, "aaaa", "ali", "reza"),
                new Message(2, "bbbb", "ali", "mohammad"), new Message(3, "cccc", "ahmad", "ali"),
                new Message(4, "dddd", "reza", "ali"), new Message(5, "eeee", "mohammad", "javad"),
                new Message(6, "ffff", "reza", "ali"), new Message(7, "gggg", "mohammad", "ali")));

        String URL = "http://localhost:" + port + "/messages/mohammad/ali";
        ResponseEntity<List<Message>> response = restTemplate.exchange(URL, HttpMethod.GET, null, new ParameterizedTypeReference<>() {
        });
        List<Message> actual = response.getBody();

        List<Message> expected = List.of(new Message("bbbb", "ali", "mohammad"),
                new Message("gggg", "mohammad", "ali"));

        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);

    }

    @Test
    public void newMessageTest() {
        HttpEntity<Message> request = new HttpEntity<>(new Message(1, "test text", "ali", "reza"));
        String URL = "http://localhost:" + port + "/messages";
        restTemplate.postForObject(URL, request, Message.class);

        List<Message> actual = ((List<Message>) messageRepository.findAll());

        List<Message> expected = new ArrayList<>(List.of(new Message(1, "test text", "ali", "reza")));

        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    public void newUserTest() {
        HttpEntity<User> request = new HttpEntity<>(new User("amin"));
        String URL = "http://localhost:" + port + "/users";
        restTemplate.postForObject(URL, request, User.class);

        List<User> actual = ((List<User>) userRepository.findAll());

        List<User> expected = new ArrayList<>(List.of(new User("amin")));

        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);

        User oldUser = restTemplate.postForObject(URL, request, User.class);

        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }
}
