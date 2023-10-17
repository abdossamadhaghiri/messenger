package com.example.server.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.model.ChatModel;
import org.example.model.UserModel;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.ArrayList;
import java.util.List;


@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class User {

    @Id
    private String username;
    private String token;

    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn
    @ManyToMany
    private List<Chat> chats;

    public UserModel toUserModel() {
        List<ChatModel> chatModels = new ArrayList<>();
        this.chats.forEach(chat -> chatModels.add(chat.toChatModel()));
        return new UserModel(this.username, this.token, chatModels);
    }

}
