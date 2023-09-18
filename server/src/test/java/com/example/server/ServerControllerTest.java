package com.example.server;

import com.example.server.repository.ChatRepository;
import com.example.server.repository.MessageRepository;
import com.example.server.repository.UserRepository;
import jakarta.annotation.Resource;
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

    @Resource
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

        Message message1 = new Message(1, "aaa", "ali", chats.get(0).getId(), 0L, null);
        Message message2 = new Message(2, "bbb", "ali", chats.get(1).getId(), 0L, null);
        Message message3 = new Message(3, "ccc", "reza", chats.get(2).getId(), 0L, null);

        Chat aliRezaChat = chats.get(0);
        Chat aliJavadChat = chats.get(1);
        Chat rezaJavadChat = chats.get(2);
        aliRezaChat.getMessages().add(message1);
        aliJavadChat.getMessages().add(message2);
        rezaJavadChat.getMessages().add(message3);
        chatRepository.saveAll(List.of(aliRezaChat, aliJavadChat, rezaJavadChat));

        User ali = new User("ali", List.of(aliRezaChat, aliJavadChat));
        User reza = new User("reza", List.of(aliRezaChat, rezaJavadChat));
        User javad = new User("javad", List.of(rezaJavadChat, aliJavadChat));
        User amir = new User("amir");
        userRepository.saveAll(List.of(ali, reza, javad, amir));
    }

    @Test
    void testNewUser_ok() {
        User user = new User("amin", new ArrayList<>());
        String URL = "http://localhost:" + port + "/users";
        client.post().uri(URL).bodyValue(user).exchange().expectStatus().isOk();
        List<User> actual = userRepository.findAll();
        assertThat(user).usingRecursiveComparison().isIn(userRepository.findAll());
    }

    @Test
    void testNewUser_invalidUsername() {
        User user = new User("amin", new ArrayList<>());
        userRepository.save(user);
        String URL = "http://localhost:" + port + "/users";
        client.post().uri(URL).bodyValue(user).exchange().expectStatus().isBadRequest();
    }

    @Test
    void testGetUser_invalidUsername() {
        String newUsername = "alireza";
        String URL = "http://localhost:" + port + "/users/" + newUsername;
        client.get().uri(URL).exchange().expectStatus().isBadRequest();
    }

    @Test
    void testGetUser_ok() {
        User expected = new User("alireza", new ArrayList<>());
        userRepository.save(expected);
        String duplicateUsername = "alireza";
        String URL = "http://localhost:" + port + "/users/" + duplicateUsername;
        client.get().uri(URL).exchange().expectStatus().isOk().expectBody(User.class).consumeWith(result ->
                assertThat(result.getResponseBody()).usingRecursiveComparison().isEqualTo(expected));
    }

    @Test
    void testNewMessage_invalidSender() {
        List<Chat> chats = chatRepository.findAll();
        Message message = new Message(4, "hello javad!", "alireza", chats.get(1).getId(), 0L, null);
        String url = "http://localhost:" + port + "/messages";
        client.post().uri(url).bodyValue(message).exchange().expectStatus().isBadRequest().expectBody(String.class).consumeWith(result ->
                assertEquals("sender username doesnt exist!", result.getResponseBody()));
    }

    @Test
    void testNewMessage_chatDoesntExists() {
        List<Chat> chats = chatRepository.findAll();
        Message message = new Message(4, "hello javad!", "ali", chats.get(2).getId() + 1, 0L, null);
        String url = "http://localhost:" + port + "/messages";
        client.post().uri(url).bodyValue(message).exchange().expectStatus().isBadRequest().expectBody(String.class).consumeWith(result ->
                assertEquals("the chat doesnt exist!", result.getResponseBody()));
    }

    @Test
    void testNewMessage_chatDoesntInSendersChat() {
        List<Chat> chats = chatRepository.findAll();
        Message message = new Message(4, "hello javad!", "ali", chats.get(2).getId(), 0L, null);
        String url = "http://localhost:" + port + "/messages";
        client.post().uri(url).bodyValue(message).exchange().expectStatus().isBadRequest().expectBody(String.class).consumeWith(result ->
                assertEquals("the chat doesnt in sender's chats!", result.getResponseBody()));
    }

    @Test
    void testNewMessage_repliedMessageDoesntExists() {
        List<Chat> chats = chatRepository.findAll();
        List<Message> messages = messageRepository.findAll();
        Message message = new Message(4, "hello javad!", "ali", chats.get(1).getId(), (long) (messages.get(2).getId() + 1), null);
        String url = "http://localhost:" + port + "/messages";
        client.post().uri(url).bodyValue(message).exchange().expectStatus().isBadRequest().expectBody(String.class).consumeWith(result ->
                assertEquals("the replied message doesnt exist!", result.getResponseBody()));
    }

    @Test
    void testNewMessage_repliedMessageDoesntInThisChat() {
        List<Chat> chats = chatRepository.findAll();
        List<Message> messages = messageRepository.findAll();
        Message message = new Message(4, "hello javad!", "ali", chats.get(1).getId(), (long) messages.get(2).getId(), null);
        String url = "http://localhost:" + port + "/messages";
        client.post().uri(url).bodyValue(message).exchange().expectStatus().isBadRequest().expectBody(String.class).consumeWith(result ->
                assertEquals("the replied message doesnt in this chat!", result.getResponseBody()));
    }

    @Test
    void testNewMessage_invalidDestination() {
        List<Chat> chats = chatRepository.findAll();
        Message message = new Message(4, "hello javad!", "ali", chats.get(1).getId(), 0L, "alireza");
        String url = "http://localhost:" + port + "/messages";
        client.post().uri(url).bodyValue(message).exchange().expectStatus().isBadRequest().expectBody(String.class).consumeWith(result ->
                assertEquals("the \"forwarded from\" username doesnt exist!", result.getResponseBody()));
    }

    @Test
    void testNewMessage_ok_notReply_notForward() {
        List<Chat> chats = chatRepository.findAll();
        List<Message> messages = messageRepository.findAll();
        Message message = new Message(messages.get(2).getId() + 1, "hello javad!", "ali", chats.get(1).getId(), 0L, null);
        String url = "http://localhost:" + port + "/messages";
        client.post().uri(url).bodyValue(message).exchange().expectStatus().isOk();
        assertThat(message).usingRecursiveComparison().isIn(messageRepository.findAll());
        assertThat(message).usingRecursiveComparison().isIn(chatRepository.findById(message.getChatId()).get().getMessages());
    }

    @Test
    void testNewMessage_ok_reply_notForward() {
        List<Chat> chats = chatRepository.findAll();
        List<Message> messages = messageRepository.findAll();
        Message message = new Message(messages.get(2).getId() + 1, "hello javad!", "ali", chats.get(1).getId(), ((long) messages.get(1).getId()), null);
        String url = "http://localhost:" + port + "/messages";
        client.post().uri(url).bodyValue(message).exchange().expectStatus().isOk();
        assertThat(message).usingRecursiveComparison().isIn(messageRepository.findAll());
        assertThat(message).usingRecursiveComparison().isIn(chatRepository.findById(message.getChatId()).get().getMessages());
    }

    @Test
    void testNewMessage_ok_notReply_forward() {
        List<Chat> chats = chatRepository.findAll();
        List<Message> messages = messageRepository.findAll();
        Message message = new Message(messages.get(2).getId() + 1, "hello javad!", "ali", chats.get(1).getId(), 0L, "javad");
        String url = "http://localhost:" + port + "/messages";
        client.post().uri(url).bodyValue(message).exchange().expectStatus().isOk();
        assertThat(message).usingRecursiveComparison().isIn(messageRepository.findAll());
        assertThat(message).usingRecursiveComparison().isIn(chatRepository.findById(message.getChatId()).get().getMessages());
    }

    @Test
    void testNewMessage_ok_reply_forward() {
        List<Chat> chats = chatRepository.findAll();
        List<Message> messages = messageRepository.findAll();
        Message message = new Message(messages.get(2).getId() + 1, "hello javad!", "ali", chats.get(1).getId(), ((long) messages.get(1).getId()), "ali");
        String url = "http://localhost:" + port + "/messages";
        client.post().uri(url).bodyValue(message).exchange().expectStatus().isOk();
        assertThat(message).usingRecursiveComparison().isIn(messageRepository.findAll());
        assertThat(message).usingRecursiveComparison().isIn(chatRepository.findById(message.getChatId()).get().getMessages());
    }

    @Test
    void testNewPv_invalidFirstUsername() {
        Chat pv = new Pv("alireza", "ali");
        String url = "http://localhost:" + port + "/chats";
        client.post().uri(url).bodyValue(pv).exchange().expectStatus().isBadRequest().expectBody(String.class).consumeWith(result ->
                assertEquals("username doesnt exist.", result.getResponseBody()));
    }

    @Test
    void testNewPv_invalidSecondUsername() {
        Chat pv = new Pv("ali", "alireza");
        String url = "http://localhost:" + port + "/chats";
        client.post().uri(url).bodyValue(pv).exchange().expectStatus().isBadRequest().expectBody(String.class).consumeWith(result ->
                assertEquals("username doesnt exist.", result.getResponseBody()));
    }

    @Test
    void testNewPv_duplicateChat() {
        Chat pv = new Pv("ali", "reza");
        String url = "http://localhost:" + port + "/chats";
        client.post().uri(url).bodyValue(pv).exchange().expectStatus().isBadRequest().expectBody(String.class).consumeWith(result ->
                assertEquals("the chat already exists.", result.getResponseBody()));
    }

    @Test
    void testNewPv_ok() {
        Chat pv = new Pv(chatRepository.findAll().get(2).getId() + 1, "ali", "amir", new ArrayList<>(), new ArrayList<>());
        String url = "http://localhost:" + port + "/chats";
        client.post().uri(url).bodyValue(pv).exchange().expectStatus().isOk();
        assertThat(pv).usingRecursiveComparison().isIn(chatRepository.findAll());
        assertThat(pv).usingRecursiveComparison().isIn(userRepository.findById("ali").get().getChats());
        assertThat(pv).usingRecursiveComparison().isIn(userRepository.findById("amir").get().getChats());
    }

    @Test
    void testNewGroup_invalidContent() {
        Group group = new Group("ali", new ArrayList<>(List.of("ali", "reza", "alireza")), "groupName");
        String url = "http://localhost:" + port + "/chats";
        client.post().uri(url).bodyValue(group).exchange().expectStatus().isBadRequest().expectBody(String.class).consumeWith(result ->
                assertEquals("invalid group content!", result.getResponseBody()));
    }

    @Test
    void testNewGroup_ok() {
        Group group = new Group(chatRepository.findAll().get(2).getId() + 1, "ali",
                new ArrayList<>(List.of("ali", "reza", "javad")), "groupName", new ArrayList<>(), new ArrayList<>());
        String url = "http://localhost:" + port + "/chats";
        client.post().uri(url).bodyValue(group).exchange().expectStatus().isOk();
        assertThat(group).usingRecursiveComparison().isIn(chatRepository.findAll());
        for (String member : group.getMembers()) {
            assertThat(group).usingRecursiveComparison().isIn(userRepository.findById(member).get().getChats());
        }
    }

    @Test
    void testGetMessage_invalidId() {
        int lastMessageId = messageRepository.findAll().get(2).getId();
        String url = "http://localhost:" + port + "/messages/" + (lastMessageId + 1);
        client.get().uri(url).exchange().expectStatus().isBadRequest();
    }

    @Test
    void testGetMessage_ok() {
        long lastMessageId = messageRepository.findAll().get(2).getId();
        Message expected = messageRepository.findById(lastMessageId).get();
        String url = "http://localhost:" + port + "/messages/" + lastMessageId;
        client.get().uri(url).exchange().expectStatus().isOk().expectBody(Message.class).consumeWith(result ->
                assertThat(result.getResponseBody()).usingRecursiveComparison().isEqualTo(expected));
    }

    @Test
    void testGetChat_invalidId() {
        long lastChatId = chatRepository.findAll().get(2).getId();
        String url = "http://localhost:" + port + "/chats/" + (lastChatId + 1);
        client.get().uri(url).exchange().expectStatus().isBadRequest();
    }

    @Test
    void testGetChat_ok() {
        long lastChatId = chatRepository.findAll().get(2).getId();
        Chat expected = chatRepository.findById(lastChatId).get();
        String url = "http://localhost:" + port + "/chats/" + lastChatId;
        client.get().uri(url).exchange().expectStatus().isOk().expectBody(Chat.class).consumeWith(result ->
                assertThat(result.getResponseBody()).usingRecursiveComparison().isEqualTo(expected));
    }

    @Test
    void testDeleteMessage_messageDoesntExists() {
        long messageId = messageRepository.findAll().get(2).getId() + 1;
        String url = "http://localhost:" + port + "/messages/" + messageId;
        client.delete().uri(url).exchange().expectStatus().isBadRequest().expectBody(String.class).consumeWith(result ->
                assertEquals("the message doesnt exist!", result.getResponseBody()));
    }

    @Test
    void testDeleteMessage_ok() {
        Message message = messageRepository.findAll().get(2);
        String url = "http://localhost:" + port + "/messages/" + message.getId();
        client.delete().uri(url).exchange().expectStatus().isOk();
        assertThat(messageRepository.findById((long) message.getId())).isEmpty();
        assertThat(message).usingRecursiveComparison().isNotIn(chatRepository.findById(message.getChatId()).get().getMessages());
    }

    @Test
    void testEditMessage_messageDoesntExists() {
        Message message = messageRepository.findAll().get(2);
        Message newMessage = new Message("edited text!", message.getSender(), message.getChatId(), message.getRepliedMessageId());
        String url = "http://localhost:" + port + "/messages/" + (message.getId() + 1);
        client.put().uri(url).bodyValue(newMessage).exchange().expectStatus().isBadRequest().expectBody(String.class).consumeWith(result ->
                assertEquals("the message doesnt exist!", result.getResponseBody()));
    }

    @Test
    void testEditMessage_ok() {
        Message message = messageRepository.findAll().get(2);
        Message newMessage = new Message("edited text!", message.getSender(), message.getChatId(), message.getRepliedMessageId());
        String url = "http://localhost:" + port + "/messages/" + message.getId();
        client.put().uri(url).bodyValue(newMessage).exchange().expectStatus().isOk();
        assertEquals(newMessage.getText(), messageRepository.findById((long) message.getId()).get().getText());
    }

    @Test
    void testPinMessage_chatDoesntExist() {
        Chat chat = chatRepository.findAll().get(2);
        String url = "http://localhost:" + port + "/chats/" + (chat.getId() + 1);
        Message pinMessage = messageRepository.findAll().get(0);
        chat.getPinMessages().add(pinMessage);
        client.put().uri(url).bodyValue(chat).exchange().expectStatus().isBadRequest().expectBody(String.class).consumeWith(result ->
                assertEquals("the chat doesnt exist!", result.getResponseBody()));
    }

    @Test
    void testPinMessage_ok() {
        Chat chat = chatRepository.findAll().get(0);
        String url = "http://localhost:" + port + "/chats/" + chat.getId();
        Message pinMessage = messageRepository.findAll().get(0);
        chat.getPinMessages().add(pinMessage);
        client.put().uri(url).bodyValue(chat).exchange().expectStatus().isOk();
        assertThat(chatRepository.findById(chat.getId()).get()).usingRecursiveComparison().isEqualTo(chat);
    }


}
