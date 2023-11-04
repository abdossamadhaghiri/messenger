package com.example.server.entity;

import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.example.model.GroupMessageModel;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@SuperBuilder
@NoArgsConstructor
public class GroupMessage extends Message {

    private Long groupId;

    public GroupMessageModel toGroupMessageModel() {
        return GroupMessageModel.builder()
                .id(this.getId())
                .text(this.getText())
                .sender(this.getSender())
                .groupId(this.groupId)
                .repliedMessageId(this.getRepliedMessageId())
                .forwardedFrom(this.getForwardedFrom())
                .build();
    }

    public static GroupMessage fromGroupMessageModel(GroupMessageModel groupMessageModel) {
        return GroupMessage.builder()
                .id(groupMessageModel.getId())
                .text(groupMessageModel.getText())
                .sender(groupMessageModel.getSender())
                .groupId(groupMessageModel.getGroupId())
                .repliedMessageId(groupMessageModel.getRepliedMessageId())
                .forwardedFrom(groupMessageModel.getForwardedFrom())
                .build();
    }

}