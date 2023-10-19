package com.example.server.entity;

import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.example.model.ChatModel;
import org.example.model.GroupModel;
import org.example.model.MessageModel;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
public class Group extends Chat {

    private String owner;

    private List<String> members;

    private String name;

    public Group(Long id, String owner, List<String> members, String name, List<Message> messages) {
        this.setId(id);
        this.owner = owner;
        this.members = members;
        this.name = name;
        this.setMessages(messages);
    }

    @Override
    public ChatModel toChatModel() {
        List<MessageModel> messageModels = new ArrayList<>();
        this.getMessages().forEach(message -> messageModels.add(message.toMessageModel()));
        return GroupModel.builder()
                .id(this.getId())
                .owner(this.owner)
                .members(this.members)
                .name(this.name)
                .messages(messageModels)
                .build();
    }
}
