package com.example.server.entity;

import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.example.model.ChatModel;
import org.example.model.MessageModel;
import org.example.model.PvModel;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
public class Pv extends Chat {

    private String first;

    private String second;

    public Pv(Long id, String first, String second, List<Message> messages) {
        this.setId(id);
        this.first = first;
        this.second = second;
        this.setMessages(messages);
    }

    @Override
    public ChatModel toChatModel() {
        List<MessageModel> messageModels = new ArrayList<>();
        this.getMessages().forEach(message -> messageModels.add(message.toMessageModel()));
        return PvModel.builder()
                .id(this.getId())
                .first(this.first)
                .second(this.second)
                .messages(messageModels)
                .build();
    }
}
