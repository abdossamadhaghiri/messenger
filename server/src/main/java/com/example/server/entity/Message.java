package com.example.server.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.example.model.MessageModel;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Builder
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private @NonNull String text;

    private @NonNull String sender;

    private @NonNull Long chatId;

    @Builder.Default
    private @NonNull Long repliedMessageId = 0L;

    public MessageModel createMessageModel() {
        return new MessageModel(this.id, this.text, this.sender, this.chatId, this.repliedMessageId);
    }

}


