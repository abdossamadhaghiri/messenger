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
}
