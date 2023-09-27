package com.example.client;

import lombok.Getter;
import org.springframework.stereotype.Component;

@Component
@Getter
public final class ApiAddresses {

    public ApiAddresses(ServerAddress serverAddress) {
        signUpApiUrl = serverAddress.getAddress() + ":" + serverAddress.getPort() + "/signUp";
        signInApiUrl = serverAddress.getAddress() + ":" + serverAddress.getPort() + "/signIn/";
        getChatsApiUrl = serverAddress.getAddress() + ":" + serverAddress.getPort() + "/chats/";
        getMessagesInOldChatApiUrl = serverAddress.getAddress() + ":" + serverAddress.getPort() + "/messages/";
        enterNewChatApiUrl = serverAddress.getAddress() + ":" + serverAddress.getPort() + "/chat";
        getChatIdApiUrl = serverAddress.getAddress() + ":" + serverAddress.getPort() + "/chatId/";
        sendMessageApiUrl = serverAddress.getAddress() + ":" + serverAddress.getPort() + "/messages";
        newGroupApiUrl = serverAddress.getAddress() + ":" + serverAddress.getPort() + "/groups";
        deleteMessageApiUrl = serverAddress.getAddress() + ":" + serverAddress.getPort() + "/messages";
        editMessageApiUrl = serverAddress.getAddress() + ":" + serverAddress.getPort() + "/messages";
        getMessageApiUrl = serverAddress.getAddress() + ":" + serverAddress.getPort() + "/messages";

    }

    private final String signUpApiUrl;
    private final String signInApiUrl;
    private final String getChatsApiUrl;
    private final String getMessagesInOldChatApiUrl;
    private final String enterNewChatApiUrl;
    private final String getChatIdApiUrl;
    private final String sendMessageApiUrl;
    private final String newGroupApiUrl;
    private final String deleteMessageApiUrl;
    private final String editMessageApiUrl;
    private final String getMessageApiUrl;

}
