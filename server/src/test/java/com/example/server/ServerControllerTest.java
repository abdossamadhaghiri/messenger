package com.example.server;

import com.example.server.entity.Message;
import com.example.server.entity.Chat;
import com.example.server.entity.Pv;
import com.example.server.entity.Group;
import com.example.server.entity.User;
import com.example.server.repository.MessageRepository;
import com.example.server.repository.ChatRepository;
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

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
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

        Message message1 = Message.builder().id(1L).text("aaa").sender("ali").chatId(chats.get(0).getId()).build();
        Message message2 = Message.builder().id(2L).text("bbb").sender("ali").chatId(chats.get(1).getId()).build();
        Message message3 = Message.builder().id(3L).text("ccc").sender("reza").chatId(chats.get(2).getId()).build();
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
        String URL = UrlPaths.SIGN_UP_API_URL;
        String newUsername = "amin";
        client.post().uri(URL).bodyValue(newUsername).exchange().expectStatus().isOk();
        assertTrue(userRepository.existsById(newUsername));
    }

    @Test
    void testSignUp_duplicateUsername() {
        userRepository.save(new User("amin", ServerController.generateToken()));
        String url = UrlPaths.SIGN_UP_API_URL;
        String duplicateUsername = "amin";
        client.post().uri(url).bodyValue(duplicateUsername).exchange().expectStatus().isBadRequest();
    }

    @Test
    void testSignIn_newUsername() {
        String newUsername = "alireza";
        String url = UrlPaths.SIGN_IN_API_URL + newUsername;
        client.get().uri(url).exchange().expectStatus().isBadRequest();
    }

    @Test
    void testSignIn_duplicateUsername() {
        userRepository.save(new User("alireza", ServerController.generateToken()));
        String duplicateUsername = "alireza";
        String url = UrlPaths.SIGN_IN_API_URL + duplicateUsername;
        client.get().uri(url).exchange().expectStatus().isOk();
    }

    @Test
    void newMessage_invalidSender() {
        List<Chat> chats = chatRepository.findAll();
        MessageModel messageModel = MessageModel.builder()
                .text("hello javad!")
                .sender("alireza")
                .chatId(chats.get(1).getId())
                .build();
        client.post()
              .uri(UrlPaths.NEW_MESSAGE_API_URL)
              .bodyValue(messageModel)
              .exchange()
              .expectStatus()
              .isBadRequest()
              .expectBody(String.class)
              .isEqualTo(Commands.USERNAME_DOESNT_EXIST);
    }

    @Test
    void newMessage_chatDoesntExists() {
        List<Chat> chats = chatRepository.findAll();
        MessageModel messageModel = MessageModel.builder()
                .text("hello javad!")
                .sender("ali")
                .chatId(chats.get(2).getId() + 1)
                .build();
        client.post()
              .uri(UrlPaths.NEW_MESSAGE_API_URL)
              .bodyValue(messageModel)
              .exchange()
              .expectStatus()
              .isBadRequest()
              .expectBody(String.class)
              .isEqualTo(Commands.CHAT_DOESNT_EXIST);
    }

    @Test
    void newMessage_chatIsNotInSendersChat() {
        List<Chat> chats = chatRepository.findAll();
        MessageModel messageModel = MessageModel.builder()
                .text("hello javad!")
                .sender("ali")
                .chatId(chats.get(2).getId())
                .build();
        client.post()
              .uri(UrlPaths.NEW_MESSAGE_API_URL)
              .bodyValue(messageModel)
              .exchange()
              .expectStatus()
              .isBadRequest()
              .expectBody(String.class)
              .isEqualTo(Commands.CHAT_IS_NOT_YOUR_CHAT);
    }

    @Test
    void newMessage_repliedMessageDoesntExists() {
        List<Chat> chats = chatRepository.findAll();
        List<Message> messages = messageRepository.findAll();
        MessageModel messageModel = MessageModel.builder()
                .text("hello javad!")
                .sender("ali")
                .chatId(chats.get(1).getId())
                .repliedMessageId(messages.get(2).getId() + 1)
                .build();
        client.post()
                .uri(UrlPaths.NEW_MESSAGE_API_URL)
                .bodyValue(messageModel)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(String.class)
                .isEqualTo(Commands.REPLIED_MESSAGE_DOESNT_EXIST);
    }

    @Test
    void newMessage_repliedMessageIsNotInThisChat() {
        List<Chat> chats = chatRepository.findAll();
        List<Message> messages = messageRepository.findAll();
        MessageModel messageModel = MessageModel.builder()
                .text("hello javad!")
                .sender("ali")
                .chatId(chats.get(1).getId())
                .repliedMessageId(messages.get(2).getId())
                .build();
        client.post()
                .uri(UrlPaths.NEW_MESSAGE_API_URL)
                .bodyValue(messageModel)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(String.class)
                .isEqualTo(Commands.REPLIED_MESSAGE_IS_NOT_IN_THIS_CHAT);
    }
    @Test
    void newMessage_forwardedFromUsernameDoesntExist() {
        List<Chat> chats = chatRepository.findAll();
        MessageModel messageModel = MessageModel.builder()
                .text("hello javad!")
                .sender("ali")
                .chatId(chats.get(1).getId())
                .forwardedFrom("alireza")
                .build();
        client.post()
              .uri(UrlPaths.NEW_MESSAGE_API_URL)
              .bodyValue(messageModel)
              .exchange()
              .expectStatus()
              .isBadRequest()
              .expectBody(String.class)
              .isEqualTo(Commands.FORWARDED_FROM_USERNAME_DOESNT_EXIST);
    }

    @Test
    void newMessage_ok_notReply_notForward() {
        List<Chat> chats = chatRepository.findAll();
        List<Message> messages = messageRepository.findAll();
        MessageModel messageModel = MessageModel.builder()
                .id(messages.get(2).getId() + 1)
                .text("hello javad!")
                .sender("ali")
                .chatId(chats.get(1).getId())
                .build();
        String url = UrlPaths.NEW_MESSAGE_API_URL;
        client.post().uri(url).bodyValue(messageModel).exchange().expectStatus().isOk();
        Message message = Message.builder()
                .id(messageModel.getId())
                .text(messageModel.getText())
                .sender(messageModel.getSender())
                .chatId(messageModel.getChatId())
                .build();
        assertThat(message).isIn(messageRepository.findAll());
    }

    @Test
    void newMessage_ok_reply_notForward() {
        List<Chat> chats = chatRepository.findAll();
        List<Message> messages = messageRepository.findAll();
        MessageModel messageModel = MessageModel.builder()
                .id(messages.get(2).getId() + 1)
                .text("hello javad!")
                .sender("ali")
                .chatId(chats.get(1).getId())
                .repliedMessageId(messages.get(1).getId())
                .build();
        client.post().uri(UrlPaths.NEW_MESSAGE_API_URL).bodyValue(messageModel).exchange().expectStatus().isOk();
        Message message = Message.builder()
                .id(messageModel.getId())
                .text(messageModel.getText())
                .sender(messageModel.getSender())
                .chatId(messageModel.getChatId())
                .repliedMessageId(messageModel.getRepliedMessageId())
                .build();
        assertThat(message).isIn(messageRepository.findAll());
    }

    @Test
    void newMessage_ok_notReply_forward() {
        List<Chat> chats = chatRepository.findAll();
        List<Message> messages = messageRepository.findAll();
        MessageModel messageModel = MessageModel.builder()
                .id(messages.get(2).getId() + 1)
                .text("hello javad!")
                .sender("ali")
                .chatId(chats.get(1).getId())
                .forwardedFrom("javad")
                .build();
        client.post().uri(UrlPaths.NEW_MESSAGE_API_URL).bodyValue(messageModel).exchange().expectStatus().isOk();
        Message message = Message.builder()
                .id(messageModel.getId())
                .text(messageModel.getText())
                .sender(messageModel.getSender())
                .chatId(messageModel.getChatId())
                .forwardedFrom(messageModel.getForwardedFrom())
                .build();
        assertThat(message).isIn(messageRepository.findAll());
    }

    @Test
    void newMessage_ok_reply_forward() {
        List<Chat> chats = chatRepository.findAll();
        List<Message> messages = messageRepository.findAll();
        MessageModel messageModel = MessageModel.builder()
                .id(messages.get(2).getId() + 1)
                .text("hello javad!")
                .sender("ali")
                .chatId(chats.get(1).getId())
                .repliedMessageId(messages.get(1).getId())
                .forwardedFrom("javad")
                .build();
        client.post().uri(UrlPaths.NEW_MESSAGE_API_URL).bodyValue(messageModel).exchange().expectStatus().isOk();
        Message message = Message.builder()
                .id(messageModel.getId())
                .text(messageModel.getText())
                .sender(messageModel.getSender())
                .chatId(messageModel.getChatId())
                .repliedMessageId(messageModel.getRepliedMessageId())
                .forwardedFrom(messageModel.getForwardedFrom())
                .build();
        assertThat(message).isIn(messageRepository.findAll());
    }

    @Test
    void testNewPv_invalidFirstUsername() {
        PvModel pvModel = new PvModel("alireza", "ali");
        String url = UrlPaths.NEW_PV_API_URL;
        client.post().uri(url).bodyValue(pvModel).exchange().expectStatus().isBadRequest().expectBody(String.class).consumeWith(result ->
                assertEquals(Commands.USERNAME_DOESNT_EXIST, result.getResponseBody()));
    }

    @Test
    void testNewPv_invalidSecondUsername() {
        PvModel pvModel = new PvModel("ali", "alireza");
        String url = UrlPaths.NEW_PV_API_URL;
        client.post().uri(url).bodyValue(pvModel).exchange().expectStatus().isBadRequest().expectBody(String.class).consumeWith(result ->
                assertEquals(Commands.USERNAME_DOESNT_EXIST, result.getResponseBody()));
    }

    @Test
    void testNewPv_duplicateChat() {
        PvModel pvModel = new PvModel("ali", "reza");
        String url = UrlPaths.NEW_PV_API_URL;
        client.post().uri(url).bodyValue(pvModel).exchange().expectStatus().isBadRequest().expectBody(String.class).consumeWith(result ->
                assertEquals(Commands.CHAT_ALREADY_EXISTS, result.getResponseBody()));
    }

    @Test
    void testNewPv_ok() {
        PvModel pvModel = new PvModel(chatRepository.findAll().get(2).getId() + 1, "ali", "amir");
        String url = UrlPaths.NEW_PV_API_URL;
        client.post().uri(url).bodyValue(pvModel).exchange().expectStatus().isOk();
        Pv pv = new Pv(pvModel.getId(), pvModel.getFirst(), pvModel.getSecond());
        assertThat(pv).isIn(chatRepository.findAll());
    }

    @Test
    void testNewGroup_invalidContent() {
        GroupModel groupModel = new GroupModel("ali", new ArrayList<>(List.of("ali", "reza", "alireza")), "groupName");
        String url = UrlPaths.NEW_GROUP_API_URL;
        client.post().uri(url).bodyValue(groupModel).exchange().expectStatus().isBadRequest().expectBody(String.class).consumeWith(result ->
                assertEquals(Commands.INVALID_GROUP_CONTENT, result.getResponseBody()));
    }

    @Test
    void testNewGroup_ok() {
        GroupModel groupModel = new GroupModel(chatRepository.findAll().get(2).getId() + 1, "ali",
                new ArrayList<>(List.of("ali", "reza", "javad")), "groupName");
        String url = UrlPaths.NEW_GROUP_API_URL;
        client.post().uri(url).bodyValue(groupModel).exchange().expectStatus().isOk();
        Group group = new Group(groupModel.getId(), groupModel.getOwner(), groupModel.getMembers(), groupModel.getName());
        assertThat(group).isIn(chatRepository.findAll());
    }

    @Test
    void testDeleteMessage_invalidToken() {
        Message message = messageRepository.findAll().get(2);
        String url = UrlPaths.DELETE_MESSAGE_API_URL + message.getId();
        client.delete().uri(url).header(HttpHeaders.AUTHORIZATION, ServerController.generateToken()).exchange().expectStatus().isBadRequest()
                .expectBody(String.class).consumeWith(result -> assertEquals(Commands.INVALID_TOKEN, result.getResponseBody()));
    }

    @Test
    void testDeleteMessage_messageDoesntExists() {
        Message message = messageRepository.findAll().get(2);
        User sender = userRepository.findById(message.getSender()).get();
        String url = UrlPaths.DELETE_MESSAGE_API_URL + (message.getId() + 1);
        client.delete().uri(url).header(HttpHeaders.AUTHORIZATION, sender.getToken()).exchange().expectStatus().isBadRequest()
                .expectBody(String.class).consumeWith(result -> assertEquals(Commands.MESSAGE_DOESNT_EXIST, result.getResponseBody()));
    }

    @Test
    void testDeleteMessage_messageDoesntYourMessage() {
        Message message = messageRepository.findAll().get(2);
        User user = userRepository.findById(messageRepository.findAll().get(1).getSender()).get();
        String url = UrlPaths.DELETE_MESSAGE_API_URL + message.getId();
        client.delete().uri(url).header(HttpHeaders.AUTHORIZATION, user.getToken()).exchange().expectStatus().isBadRequest()
                .expectBody(String.class).consumeWith(result -> assertEquals(Commands.MESSAGE_IS_NOT_YOUR_MESSAGE, result.getResponseBody()));
    }

    @Test
    void testDeleteMessage_ok() {
        Message message = messageRepository.findAll().get(2);
        User sender = userRepository.findById(message.getSender()).get();
        String url = UrlPaths.DELETE_MESSAGE_API_URL + message.getId();
        client.delete().uri(url).header(HttpHeaders.AUTHORIZATION, sender.getToken()).exchange().expectStatus().isOk();
        assertThat(messageRepository.findById(message.getId())).isEmpty();
    }

    @Test
    void editMessage_invalidToken() {
        Message message = messageRepository.findAll().get(2);
        MessageModel messageModel = MessageModel.builder()
                                                .id(message.getId())
                                                .text("edited text!")
                                                .sender(message.getSender())
                                                .chatId(message.getChatId())
                                                .build();
        String url = UrlPaths.EDIT_MESSAGE_API_URL + message.getId();
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
        Message message = messageRepository.findAll().get(2);
        User sender = userRepository.findById(message.getSender()).get();
        MessageModel messageModel = MessageModel.builder()
                                                .id(message.getId())
                                                .text("edited text!")
                                                .sender(message.getSender())
                                                .chatId(message.getChatId())
                                                .build();
        String url = UrlPaths.EDIT_MESSAGE_API_URL + (message.getId() + 1);
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
        Message message = messageRepository.findAll().get(2);
        MessageModel messageModel = MessageModel.builder()
                                                .id(message.getId())
                                                .text("edited text!")
                                                .sender(message.getSender())
                                                .chatId(message.getChatId())
                                                .build();
        User user = userRepository.findById(messageRepository.findAll().get(1).getSender()).get();
        String url = UrlPaths.EDIT_MESSAGE_API_URL + message.getId();
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
        Message message = messageRepository.findAll().get(2);
        MessageModel messageModel = MessageModel.builder()
                                                .id(message.getId())
                                                .text("edited text!")
                                                .sender(message.getSender())
                                                .chatId(message.getChatId())
                                                .build();
        User sender = userRepository.findById(message.getSender()).get();
        String url = UrlPaths.EDIT_MESSAGE_API_URL + message.getId();
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
        Message message = messageRepository.findAll().get(2);
        String url = UrlPaths.GET_MESSAGE_API_URL + message.getId();
        client.get().uri(url).header(HttpHeaders.AUTHORIZATION, ServerController.generateToken()).exchange().expectStatus().isBadRequest();
    }

    @Test
    void testGetMessage_messageDoesntExists() {
        Message message = messageRepository.findAll().get(2);
        User sender = userRepository.findById(message.getSender()).get();
        String url = UrlPaths.GET_MESSAGE_API_URL + (message.getId() + 1);
        client.get().uri(url).header(HttpHeaders.AUTHORIZATION, sender.getToken()).exchange().expectStatus().isBadRequest();
    }

    @Test
    void getMessage_messageIsNotInYourChats() {
        Message message = messageRepository.findAll().get(2);
        User user = userRepository.findById("ali").get();
        String url = UrlPaths.GET_MESSAGE_API_URL + message.getId();
        client.get()
              .uri(url)
              .header(HttpHeaders.AUTHORIZATION, user.getToken())
              .exchange()
              .expectStatus()
              .isBadRequest();
    }

    @Test
    void testGetMessage_ok() {
        Message message = messageRepository.findAll().get(2);
        User user = userRepository.findById("javad").get();
        MessageModel expected = message.createMessageModel();
        String url = UrlPaths.GET_MESSAGE_API_URL + message.getId();
        client.get()
              .uri(url)
              .header(HttpHeaders.AUTHORIZATION, user.getToken())
              .exchange()
              .expectStatus()
              .isOk()
              .expectBody(MessageModel.class)
              .isEqualTo(expected);
    }


}
