package org.example.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.List;


@Entity
@ToString
@NoArgsConstructor
@RequiredArgsConstructor
@Table(name = "tttt")
public class User {

    @Id
    private @NonNull @Getter String username;

    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn
    @ManyToMany
    private @Getter List<Chat> chats;

}
