package com.example.server;

import org.example.Message;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DatabaseTest {

    @Test
    public void getMessagesInChatTest() {
        Database database = new Database(List.of(new Message("aaaa", "ali", "reza"),
                new Message("bbbb", "ali", "mohammad"), new Message("cccc", "ahmad", "ali"),
                new Message("dddd", "reza", "ali"), new Message("eeee", "mohammad", "javad"),
                new Message("ffff", "reza", "ali"), new Message("gggg", "mohammad", "ahmad")), null);

        List<Message> expected = new ArrayList<>(List.of(database.getAllMessages().get(0), database.getAllMessages().get(3),
                database.getAllMessages().get(5)));

        List<Message> actual = database.getMessagesInChat("reza", "ali");

        assertEquals(expected, actual);
    }

}
