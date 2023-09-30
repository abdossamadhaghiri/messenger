package com.example.client;

import lombok.Getter;
import org.example.UrlPaths;
import org.springframework.stereotype.Component;

@Component
@Getter
public final class ApiAddresses {

    public ApiAddresses(ServerAddress serverAddress) {
        signUpApiUrl = serverAddress.getAddress() + ":" + serverAddress.getPort() + UrlPaths.SIGN_UP_API_URL;
        signInApiUrl = serverAddress.getAddress() + ":" + serverAddress.getPort() + UrlPaths.SIGN_IN_API_URL;
        getChatsApiUrl = serverAddress.getAddress() + ":" + serverAddress.getPort() + UrlPaths.GET_CHATS_API_URL;
        getMessagesInOldChatApiUrl = serverAddress.getAddress() + ":" + serverAddress.getPort() + UrlPaths.GET_MESSAGE_API_URL;
        enterNewChatApiUrl = serverAddress.getAddress() + ":" + serverAddress.getPort() + UrlPaths.NEW_PV_API_URL;
        getChatIdApiUrl = serverAddress.getAddress() + ":" + serverAddress.getPort() + UrlPaths.GET_CHAT_ID_API_URL;
        sendMessageApiUrl = serverAddress.getAddress() + ":" + serverAddress.getPort() + UrlPaths.NEW_MESSAGE_API_URL;
        newGroupApiUrl = serverAddress.getAddress() + ":" + serverAddress.getPort() + UrlPaths.NEW_GROUP_API_URL;
        deleteMessageApiUrl = serverAddress.getAddress() + ":" + serverAddress.getPort() + UrlPaths.DELETE_MESSAGE_API_URL;
        editMessageApiUrl = serverAddress.getAddress() + ":" + serverAddress.getPort() + UrlPaths.EDIT_MESSAGE_API_URL;
        getMessageApiUrl = serverAddress.getAddress() + ":" + serverAddress.getPort() + UrlPaths.GET_MESSAGE_API_URL;

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
