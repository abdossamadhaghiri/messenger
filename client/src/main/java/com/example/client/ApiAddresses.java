package com.example.client;

import lombok.Getter;
import org.example.UrlPaths;
import org.springframework.stereotype.Component;

@Component
@Getter
public final class ApiAddresses {

    public ApiAddresses(ServerAddress serverAddress) {
        usersApiUrl = serverAddress.getAddress() + ":" + serverAddress.getPort() + UrlPaths.USERS_URL_PATH;
        chatsApiUrl = serverAddress.getAddress() + ":" + serverAddress.getPort() + UrlPaths.CHATS_URL_PATH;
        messagesApiUrl = serverAddress.getAddress() + ":" + serverAddress.getPort() + UrlPaths.MESSAGES_URL_PATH;

    }

    private final String usersApiUrl;
    private final String chatsApiUrl;
    private final String messagesApiUrl;

}
