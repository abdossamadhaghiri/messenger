package org.example.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@NoArgsConstructor
@Getter
@SuperBuilder
public class GroupModel extends ChatModel {

    private String owner;
    private List<String> members;
    private String name;


}
