package com.example.client;

import lombok.Getter;
import org.springframework.stereotype.Component;

@Component
@Getter
public final class ApiAddresses {

    public ApiAddresses(ServerAddress serverAddress) {
        baseUrl = serverAddress.getAddress() + ":" + serverAddress.getPort();
    }

    private final String baseUrl;

}
