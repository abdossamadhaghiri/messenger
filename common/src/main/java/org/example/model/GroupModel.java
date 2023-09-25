package org.example.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
public class GroupModel extends ChatModel {

    private @Getter String owner;
    private @Getter List<String> members;
    private @Getter String name;

    public GroupModel(Long id, String owner, List<String> members, String name) {
        this.setId(id);
        this.owner = owner;
        this.members = members;
        this.name = name;
    }
}
