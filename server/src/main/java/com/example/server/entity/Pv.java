package com.example.server.entity;

import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
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
@SuperBuilder
public class Pv extends Chat {

    private String first;

    private String second;

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

    public static Pv fromPvModel(PvModel pvModel) {
        List<Message> messages = new ArrayList<>();
        pvModel.getMessages().forEach(messageModel -> messages.add(Message.fromMessageModel(messageModel)));
        return Pv.builder()
                .id(pvModel.getId())
                .first(pvModel.getFirst())
                .second(pvModel.getSecond())
                .messages(messages)
                .build();
    }
}
