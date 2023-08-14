package com.example.server;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class ApplicationConfig {

    @Bean(name = "emptyDatabase")
    public Database getEmptyDatabase() {
        return new Database();
    }
}
