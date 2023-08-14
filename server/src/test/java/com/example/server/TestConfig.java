package com.example.server;

import org.example.Message;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class TestConfig {

    @Primary
    @Bean(name = "testDatabase")
    public Database getTestDatabase() {
        return new Database(new ArrayList<>(List.of(new Message("aaaa", "ali", "reza"),
                new Message("bbbb", "ali", "mohammad"), new Message("cccc", "ahmad", "ali"),
                new Message("dddd", "reza", "ali"), new Message("eeee", "mohammad", "javad"),
                new Message("ffff", "reza", "ali"), new Message("gggg", "mohammad", "ali"))),
                new ArrayList<>( List.of("ali", "reza", "mohammad", "ahmad", "javad")));
    }
}
