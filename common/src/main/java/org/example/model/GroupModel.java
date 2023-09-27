package org.example.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class GroupModel extends ChatModel {

    private String owner;
    private List<String> members;
    private String name;

    public GroupModel(Long id, String owner, List<String> members, String name) {
        this.setId(id);
        this.owner = owner;
        this.members = members;
        this.name = name;
    }
}
