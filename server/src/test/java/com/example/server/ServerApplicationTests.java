package com.example.server;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ServerApplicationTests {

    @Autowired
    private ServerController serverController;
    @Test
    void contextLoads() {
        assertThat(serverController).isNotNull();
    }

}
