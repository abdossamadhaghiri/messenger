package org.example.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@Getter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class GroupModel extends ChatModel {

    private String owner;
    private List<String> members;
    private String name;
    @Builder.Default
    private List<GroupMessageModel> groupMessages = new ArrayList<>();

}
