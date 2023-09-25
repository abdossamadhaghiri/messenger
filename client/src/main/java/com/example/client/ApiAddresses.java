package com.example.client;

import org.springframework.stereotype.Component;

@Component
public class ApiAddresses {

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

    }

    public String signUpApiUrl;
    public String signInApiUrl;
    public String getChatsApiUrl;
    public String getMessagesInOldChatApiUrl;
    public String enterNewChatApiUrl;
    public String getChatIdApiUrl;
    public String sendMessageApiUrl;
    public String newGroupApiUrl;
    public String deleteMessageApiUrl;
    public String editMessageApiUrl;


}
