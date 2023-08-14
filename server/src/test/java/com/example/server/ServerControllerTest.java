package com.example.server;

import org.example.Message;
import org.example.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ServerControllerTest {

    @Value(value = "${local.server.port}")
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void getUsersTest() {
        List<String> expected = List.of("ali", "reza", "mohammad", "ahmad", "javad");
        List<String> actual = restTemplate.getForObject("http://localhost:" + port + "/users", List.class);

        assertEquals(expected, actual);
    }

    @Test
    public void getMessagesInChatTest() {
        List<Message> expected = List.of(new Message("bbbb", "ali", "mohammad"),
                new Message("gggg", "mohammad", "ali"));

        String URL = "http://localhost:" + port + "/messages/mohammad/ali";
        ResponseEntity<List<Message>> response = restTemplate.exchange(URL, HttpMethod.GET, null, new ParameterizedTypeReference<>() {
        });
        List<Message> actual = response.getBody();

        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);

    }

    @Test
    public void newMessageTest() {
        HttpEntity<Message> request = new HttpEntity<>(new Message("test text", "ali", "reza"));
        String URL = "http://localhost:" + port + "/messages";
        Message actual = restTemplate.postForObject(URL, request, Message.class);

        Message expected = new Message("test text", "ali", "reza");

        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    public void newUserTest() {
        HttpEntity<User> request = new HttpEntity<>(new User("amin"));
        String URL = "http://localhost:" + port + "/users";
        User actual = restTemplate.postForObject(URL, request, User.class);

        User expected = new User("amin");

        assertEquals(expected, actual);

        User user = restTemplate.postForObject(URL, request, User.class);

        assertEquals(expected, user);
    }
}
