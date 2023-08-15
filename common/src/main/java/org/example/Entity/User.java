package org.example.Entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;


@Entity
@Table(name = "users")
public class User {

    @Id
    private String username;

    public User(String username) {
        this.username = username;
    }

    protected User() {
    }

    public String getUsername() {
        return username;
    }


}
