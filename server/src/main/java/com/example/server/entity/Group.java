package com.example.server.entity;

import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.example.model.GroupModel;
import org.example.model.MessageModel;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Data
@Entity
@SuperBuilder
public class Group extends Chat {

    private String owner;

    private List<String> members;

    private String name;

    public GroupModel toGroupModel() {
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

    public static Group fromGroupModel(GroupModel groupModel) {
        List<Message> messages = new ArrayList<>();
        groupModel.getMessages().forEach(messageModel -> messages.add(Message.fromMessageModel(messageModel)));
        return Group.builder()
                .id(groupModel.getId())
                .owner(groupModel.getOwner())
                .members(groupModel.getMembers())
                .name(groupModel.getName())
                .messages(messages)
                .build();
    }
}
