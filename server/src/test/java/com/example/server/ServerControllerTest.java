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
import org.example.model.ChatModel;
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
    private MessageRepository messageRepository;

    @Autowired
    private ChatRepository chatRepository;

    private List<Chat> chats;
    private List<Message> messages;

    @BeforeEach
    public void setUpTestDatabase() {
        clearDatabase();
        createTestDatabase();
        chats = chatRepository.findAll();
        messages = messageRepository.findAll();
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

        Chat aliRezaChat = chats.get(0);
        Chat aliJavadChat = chats.get(1);
        Chat rezaJavadChat = chats.get(2);
        aliRezaChat.getMessages().add(message1);
        aliJavadChat.getMessages().add(message2);
        rezaJavadChat.getMessages().add(message3);
        chatRepository.saveAll(List.of(aliRezaChat, aliJavadChat, rezaJavadChat));

        User ali = new User("ali", ServerController.generateToken(), new ArrayList<>(List.of(aliRezaChat, aliJavadChat)));
        User reza = new User("reza", ServerController.generateToken(), new ArrayList<>(List.of(aliRezaChat, rezaJavadChat)));
        User javad = new User("javad", ServerController.generateToken(), new ArrayList<>(List.of(aliJavadChat, rezaJavadChat)));
        User amir = new User("amir", ServerController.generateToken(), new ArrayList<>());
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
        userRepository.save(new User("amin", ServerController.generateToken(), new ArrayList<>()));
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
        userRepository.save(new User("alireza", ServerController.generateToken(), new ArrayList<>()));
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
        userRepository.save(new User("alireza", ServerController.generateToken(), new ArrayList<>()));
        String duplicateUsername = "alireza";
        String url = UrlPaths.USERS_URL_PATH + SLASH + duplicateUsername;
        client.get().uri(url).exchange().expectStatus().isOk();
    }

    @Test
    void newMessage_invalidSender() {
        MessageModel messageModel = MessageModel.builder()
                .text("hello javad!")
                .sender("alireza")
                .chatId(chats.get(1).getId())
                .build();
        client.post()
                .uri(UrlPaths.MESSAGES_URL_PATH)
                .bodyValue(messageModel)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(String.class)
                .isEqualTo(Commands.USERNAME_DOESNT_EXIST);
    }

    @Test
    void newMessage_chatDoesntExists() {
        MessageModel messageModel = MessageModel.builder()
                .text("hello javad!")
                .sender("ali")
                .chatId(chats.get(2).getId() + 1)
                .build();
        client.post()
                .uri(UrlPaths.MESSAGES_URL_PATH)
                .bodyValue(messageModel)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(String.class)
                .isEqualTo(Commands.CHAT_DOESNT_EXIST);
    }

    @Test
    void newMessage_chatIsNotInSendersChat() {
        MessageModel messageModel = MessageModel.builder()
                .text("hello javad!")
                .sender("ali")
                .chatId(chats.get(2).getId())
                .build();
        client.post()
                .uri(UrlPaths.MESSAGES_URL_PATH)
                .bodyValue(messageModel)
                .exchange()
                .expectStatus()
                .isForbidden()
                .expectBody(String.class)
                .isEqualTo(Commands.CHAT_IS_NOT_YOUR_CHAT);
    }

    @Test
    void newMessage_repliedMessageDoesntExists() {
        MessageModel messageModel = MessageModel.builder()
                .text("hello javad!")
                .sender("ali")
                .chatId(chats.get(1).getId())
                .repliedMessageId(messages.get(2).getId() + 1)
                .build();
        client.post()
                .uri(UrlPaths.MESSAGES_URL_PATH)
                .bodyValue(messageModel)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(String.class)
                .isEqualTo(Commands.REPLIED_MESSAGE_DOESNT_EXIST);
    }

    @Test
    void newMessage_repliedMessageIsNotInThisChat() {
        MessageModel messageModel = MessageModel.builder()
                .text("hello javad!")
                .sender("ali")
                .chatId(chats.get(1).getId())
                .repliedMessageId(messages.get(2).getId())
                .build();
        client.post()
                .uri(UrlPaths.MESSAGES_URL_PATH)
                .bodyValue(messageModel)
                .exchange()
                .expectStatus()
                .isForbidden()
                .expectBody(String.class)
                .isEqualTo(Commands.REPLIED_MESSAGE_IS_NOT_IN_THIS_CHAT);
    }

    @Test
    void newMessage_forwardedFromUsernameDoesntExist() {
        MessageModel messageModel = MessageModel.builder()
                .text("hello javad!")
                .sender("ali")
                .chatId(chats.get(1).getId())
                .forwardedFrom("alireza")
                .build();
        client.post()
                .uri(UrlPaths.MESSAGES_URL_PATH)
                .bodyValue(messageModel)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(String.class)
                .isEqualTo(Commands.FORWARDED_FROM_USERNAME_DOESNT_EXIST);
    }

    @Test
    void newMessage_ok_notReply_notForward() {
        MessageModel messageModel = MessageModel.builder()
                .id(messages.get(2).getId() + 1)
                .text("hello javad!")
                .sender("ali")
                .chatId(chats.get(1).getId())
                .build();
        String url = UrlPaths.MESSAGES_URL_PATH;
        client.post().uri(url).bodyValue(messageModel).exchange().expectStatus().isOk();
        Message message = Message.fromMessageModel(messageModel);
        assertThat(message).isIn(messageRepository.findAll()).isIn(chatRepository.findById(message.getChatId()).get().getMessages());
    }

    @Test
    void newMessage_ok_reply_notForward() {
        MessageModel messageModel = MessageModel.builder()
                .id(messages.get(2).getId() + 1)
                .text("hello javad!")
                .sender("ali")
                .chatId(chats.get(1).getId())
                .repliedMessageId(messages.get(1).getId())
                .build();
        client.post().uri(UrlPaths.MESSAGES_URL_PATH).bodyValue(messageModel).exchange().expectStatus().isOk();
        Message message = Message.fromMessageModel(messageModel);
        assertThat(message).isIn(messageRepository.findAll()).isIn(chatRepository.findById(message.getChatId()).get().getMessages());
    }

    @Test
    void newMessage_ok_notReply_forward() {
        MessageModel messageModel = MessageModel.builder()
                .id(messages.get(2).getId() + 1)
                .text("hello javad!")
                .sender("ali")
                .chatId(chats.get(1).getId())
                .forwardedFrom("javad")
                .build();
        client.post().uri(UrlPaths.MESSAGES_URL_PATH).bodyValue(messageModel).exchange().expectStatus().isOk();
        Message message = Message.fromMessageModel(messageModel);
        assertThat(message).isIn(messageRepository.findAll()).isIn(chatRepository.findById(message.getChatId()).get().getMessages());
    }

    @Test
    void newMessage_ok_reply_forward() {
        MessageModel messageModel = MessageModel.builder()
                .id(messages.get(2).getId() + 1)
                .text("hello javad!")
                .sender("ali")
                .chatId(chats.get(1).getId())
                .repliedMessageId(messages.get(1).getId())
                .forwardedFrom("javad")
                .build();
        client.post().uri(UrlPaths.MESSAGES_URL_PATH).bodyValue(messageModel).exchange().expectStatus().isOk();
        Message message = Message.fromMessageModel(messageModel);
        assertThat(message).isIn(messageRepository.findAll()).isIn(chatRepository.findById(message.getChatId()).get().getMessages());
    }

    @Test
    void testNewPv_invalidFirstUsername() {
        PvModel pvModel = PvModel.builder().first("alireza").second("ali").build();
        String url = UrlPaths.CHATS_URL_PATH;
        client.post()
                .uri(url)
                .bodyValue(pvModel)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(String.class)
                .isEqualTo(null);
    }

    @Test
    void testNewPv_invalidSecondUsername() {
        PvModel pvModel = PvModel.builder().first("ali").second("alireza").build();
        String url = UrlPaths.CHATS_URL_PATH;
        client.post()
                .uri(url)
                .bodyValue(pvModel)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(String.class)
                .isEqualTo(null);
    }

    @Test
    void testNewPv_duplicateChat() {
        PvModel pvModel = PvModel.builder().first("ali").second("reza").build();
        String url = UrlPaths.CHATS_URL_PATH;
        client.post()
                .uri(url)
                .bodyValue(pvModel)
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(String.class)
                .isEqualTo(null);
    }

    @Test
    void testNewPv_ok() {
        PvModel pvModel = PvModel.builder().id(chats.get(2).getId() + 1).first("ali").second("amir").build();
        String url = UrlPaths.CHATS_URL_PATH;
        client.post().uri(url).bodyValue(pvModel).exchange().expectStatus().isCreated();
        Pv pv = Pv.fromPvModel(pvModel);
        assertThat(pv).isIn(chatRepository.findAll()).isIn(userRepository.findById(pv.getFirst()).get().getChats())
                .isIn(userRepository.findById(pv.getSecond()).get().getChats());
    }

    @Test
    void testNewGroup_invalidContent() {
        GroupModel groupModel = GroupModel.builder()
                .owner("ali")
                .members(new ArrayList<>(List.of("ali", "reza", "alireza")))
                .name("groupName")
                .build();
        String url = UrlPaths.CHATS_URL_PATH;
        client.post()
                .uri(url)
                .bodyValue(groupModel)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(String.class)
                .isEqualTo(null);
    }

    @Test
    void testNewGroup_ok() {
        GroupModel groupModel = GroupModel.builder()
                .id(chats.get(2).getId() + 1)
                .owner("ali")
                .members(new ArrayList<>(List.of("ali", "reza", "javad")))
                .name("groupName")
                .build();
        String url = UrlPaths.CHATS_URL_PATH;
        client.post().uri(url).bodyValue(groupModel).exchange().expectStatus().isCreated();
        Group group = Group.fromGroupModel(groupModel);
        assertThat(group).isIn(chatRepository.findAll());
        group.getMembers().forEach(member -> assertThat(group).isIn(userRepository.findById(member).get().getChats()));
    }

    @Test
    void testDeleteMessage_invalidToken() {
        Message message = messages.get(2);
        String url = UrlPaths.MESSAGES_URL_PATH + SLASH + message.getId();
        client.delete().uri(url).header(HttpHeaders.AUTHORIZATION, ServerController.generateToken()).exchange().expectStatus().isUnauthorized()
                .expectBody(String.class).consumeWith(result -> assertEquals(Commands.INVALID_TOKEN, result.getResponseBody()));
    }

    @Test
    void testDeleteMessage_messageDoesntExists() {
        Message message = messages.get(2);
        User sender = userRepository.findById(message.getSender()).get();
        String url = UrlPaths.MESSAGES_URL_PATH + SLASH + (message.getId() + 1);
        client.delete().uri(url).header(HttpHeaders.AUTHORIZATION, sender.getToken()).exchange().expectStatus().isNotFound()
                .expectBody(String.class).consumeWith(result -> assertEquals(Commands.MESSAGE_DOESNT_EXIST, result.getResponseBody()));
    }

    @Test
    void testDeleteMessage_messageDoesntYourMessage() {
        Message message = messages.get(2);
        User user = userRepository.findById(messages.get(1).getSender()).get();
        String url = UrlPaths.MESSAGES_URL_PATH + SLASH + message.getId();
        client.delete().uri(url).header(HttpHeaders.AUTHORIZATION, user.getToken()).exchange().expectStatus().isForbidden()
                .expectBody(String.class).consumeWith(result -> assertEquals(Commands.MESSAGE_IS_NOT_YOUR_MESSAGE, result.getResponseBody()));
    }

    @Test
    void testDeleteMessage_ok() {
        Message message = messages.get(2);
        User sender = userRepository.findById(message.getSender()).get();
        String url = UrlPaths.MESSAGES_URL_PATH + SLASH + message.getId();
        client.delete().uri(url).header(HttpHeaders.AUTHORIZATION, sender.getToken()).exchange().expectStatus().isOk();
        assertThat(message).isNotIn(messageRepository.findAll()).isNotIn(chatRepository.findById(message.getChatId()).get().getMessages());
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
        String url = UrlPaths.MESSAGES_URL_PATH + SLASH + message.getId();
        client.put()
              .uri(url)
              .header(HttpHeaders.AUTHORIZATION, ServerController.generateToken())
              .bodyValue(messageModel)
              .exchange()
              .expectStatus()
              .isUnauthorized()
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
        String url = UrlPaths.MESSAGES_URL_PATH + SLASH + (message.getId() + 1);
        client.put()
              .uri(url)
              .header(HttpHeaders.AUTHORIZATION, sender.getToken())
              .bodyValue(messageModel)
              .exchange()
              .expectStatus()
              .isNotFound()
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
        String url = UrlPaths.MESSAGES_URL_PATH + SLASH + message.getId();
        client.put()
              .uri(url)
              .header(HttpHeaders.AUTHORIZATION, user.getToken())
              .bodyValue(messageModel)
              .exchange()
              .expectStatus()
              .isForbidden()
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
        String url = UrlPaths.MESSAGES_URL_PATH + SLASH + message.getId();
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
        String url = UrlPaths.MESSAGES_URL_PATH + SLASH + message.getId();
        client.get().uri(url).header(HttpHeaders.AUTHORIZATION, ServerController.generateToken()).exchange().expectStatus().isUnauthorized();
    }

    @Test
    void testGetMessage_messageDoesntExists() {
        Message message = messages.get(2);
        User sender = userRepository.findById(message.getSender()).get();
        String url = UrlPaths.MESSAGES_URL_PATH + SLASH + (message.getId() + 1);
        client.get().uri(url).header(HttpHeaders.AUTHORIZATION, sender.getToken()).exchange().expectStatus().isNotFound();
    }

    @Test
    void getMessage_messageIsNotInYourChats() {
        Message message = messages.get(2);
        User user = userRepository.findById("ali").get();
        String url = UrlPaths.MESSAGES_URL_PATH + SLASH + message.getId();
        client.get()
              .uri(url)
              .header(HttpHeaders.AUTHORIZATION, user.getToken())
              .exchange()
              .expectStatus()
              .isForbidden();
    }

    @Test
    void testGetMessage_ok() {
        Message message = messages.get(2);
        User user = userRepository.findById("javad").get();
        MessageModel expected = message.toMessageModel();
        String url = UrlPaths.MESSAGES_URL_PATH + SLASH + message.getId();
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
    void getChat_invalidToken() {
        client.get()
                .uri(UrlPaths.CHATS_URL_PATH + SLASH + chats.get(0).getId())
                .header(HttpHeaders.AUTHORIZATION, ServerController.generateToken())
                .exchange()
                .expectStatus()
                .isUnauthorized();
    }

    @Test
    void getChat_chatDoesntExist() {
        client.get()
                .uri(UrlPaths.CHATS_URL_PATH + SLASH + (chats.get(2).getId() + 1))
                .header(HttpHeaders.AUTHORIZATION, userRepository.findById("ali").get().getToken())
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    @Test
    void getChat_chatIsNotInYourChats() {
        client.get()
                .uri(UrlPaths.CHATS_URL_PATH + SLASH + chats.get(0).getId())
                .header(HttpHeaders.AUTHORIZATION, userRepository.findById("javad").get().getToken())
                .exchange()
                .expectStatus()
                .isForbidden();
    }

    @Test
    void getChat_ok() {
        client.get()
                .uri(UrlPaths.CHATS_URL_PATH + SLASH + chats.get(0).getId())
                .header(HttpHeaders.AUTHORIZATION, userRepository.findById("ali").get().getToken())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ChatModel.class)
                .isEqualTo(chats.get(0).toChatModel());
    }

}
