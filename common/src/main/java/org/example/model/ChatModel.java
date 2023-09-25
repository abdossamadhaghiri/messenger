package org.example.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = PvModel.class, name = "pvModel"),
        @JsonSubTypes.Type(value = GroupModel.class, name = "groupModel")
})
public abstract class ChatModel {

    private @Getter @Setter Long id;

}
