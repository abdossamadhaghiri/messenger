package com.example.client;

import lombok.Getter;
import org.example.UrlPaths;
import org.springframework.stereotype.Component;

@Component
@Getter
public final class ApiAddresses {

    public ApiAddresses(ServerAddress serverAddress) {
        usersApiUrl = serverAddress.getAddress() + ":" + serverAddress.getPort() + UrlPaths.USERS_URL_PATH;
        pvsApiUrl = serverAddress.getAddress() + ":" + serverAddress.getPort() + UrlPaths.PVS_URL_PATH;
        groupsApiUrl = serverAddress.getAddress() + ":" + serverAddress.getPort() + UrlPaths.GROUPS_URL_PATH;
        pvMessagesApiUrl = serverAddress.getAddress() + ":" + serverAddress.getPort() + UrlPaths.PV_MESSAGES_URL_PATH;
        groupMessagesApiUrl = serverAddress.getAddress() + ":" + serverAddress.getPort() + UrlPaths.GROUP_MESSAGES_URL_PATH;
    }

    private final String usersApiUrl;
    private final String pvsApiUrl;
    private final String groupsApiUrl;
    private final String pvMessagesApiUrl;
    private final String groupMessagesApiUrl;


}