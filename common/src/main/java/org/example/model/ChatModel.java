package org.example.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = PvModel.class, name = "pvModel"),
        @JsonSubTypes.Type(value = GroupModel.class, name = "groupModel")
})
@Getter
public abstract class ChatModel {

    private @Setter Long id;
    private List<MessageModel> messages;
}
