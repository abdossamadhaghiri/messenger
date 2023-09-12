package org.example.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;


@Entity
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "table_user")
public class User {

    @Id
    private @Getter String username;

}
