package com.example.client;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ClientApplication {

    private final CommandProcessor commandProcessor;

    public static void main(String[] args) {
        SpringApplication.run(ClientApplication.class, args);
    }

    public ClientApplication(CommandProcessor commandProcessor) {
        this.commandProcessor = commandProcessor;
    }

    @Bean
    public CommandLineRunner run() {
        return args -> commandProcessor.run();
    }
}
