package com.example.client;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ServerAddress {

    @Value("${server.address}")
    private @Getter @Setter String address;

    @Value("${server.port}")
    private @Getter @Setter int port;

}
