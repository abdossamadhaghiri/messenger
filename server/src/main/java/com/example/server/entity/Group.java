package com.example.server.entity;

import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.example.model.ChatModel;
import org.example.model.GroupModel;

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

    public Group(Long id, String owner, List<String> members, String name) {
        this.setId(id);
        this.owner = owner;
        this.members = members;
        this.name = name;
    }

    @Override
    public ChatModel toChatModel() {
        return new GroupModel(this.getId(), this.owner, this.members, this.name);
    }
}
