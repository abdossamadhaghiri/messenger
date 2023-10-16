package org.example.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = PvModel.class, name = "pvModel"),
        @JsonSubTypes.Type(value = GroupModel.class, name = "groupModel")
})
@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode
public abstract class ChatModel {

    private @Setter Long id;
    @Builder.Default
    private List<MessageModel> messages = new ArrayList<>();
}
