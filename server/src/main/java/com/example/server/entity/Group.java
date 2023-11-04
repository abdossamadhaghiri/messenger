package com.example.server.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.example.model.GroupMessageModel;
import org.example.model.GroupModel;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

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

    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn
    @OneToMany(cascade = CascadeType.ALL)
    @Builder.Default
    private List<GroupMessage> groupMessages = new ArrayList<>();

    public GroupModel toGroupModel() {
        List<GroupMessageModel> groupMessageModels = new ArrayList<>();
        this.groupMessages.forEach(groupMessage -> groupMessageModels.add(groupMessage.toGroupMessageModel()));
        return GroupModel.builder()
                .id(this.getId())
                .owner(this.owner)
                .members(this.members)
                .name(this.name)
                .groupMessages(groupMessageModels)
                .build();
    }

    public static Group fromGroupModel(GroupModel groupModel) {
        List<GroupMessage> groupMessages = new ArrayList<>();
        groupModel.getGroupMessages().forEach(groupMessageModel -> groupMessages.add(GroupMessage.fromGroupMessageModel(groupMessageModel)));
        return Group.builder()
                .id(groupModel.getId())
                .owner(groupModel.getOwner())
                .members(groupModel.getMembers())
                .name(groupModel.getName())
                .groupMessages(groupMessages)
                .build();
    }
}
