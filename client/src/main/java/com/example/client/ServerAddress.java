package com.example.client;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "server")
public class ServerAddress {

    private @Getter @Setter String address;

    private @Getter @Setter int port;

}
