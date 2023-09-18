package org.example.entity;

import jakarta.persistence.Entity;
import lombok.*;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
public class Group extends Chat {

    private @Getter @Setter String owner;

    private @Getter @Setter List<String> members;

    private @Getter @Setter String name;

    public Group(Long id, String owner, List<String> members, String name, List<Message> messages, List<Message> pinMessages) {
        this.setId(id);
        this.owner = owner;
        this.members = members;
        this.name = name;
        this.setMessages(messages);
        this.setPinMessages(pinMessages);
    }
}
