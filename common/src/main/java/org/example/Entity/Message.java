package org.example.Entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@ToString
@Entity
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private @Getter String text;

    private @Getter String sender;

    private @Getter String receiver;

    public Message(String text, String sender, String receiver) {
        this.id = 1;
        this.text = text;
        this.sender = sender;
        this.receiver = receiver;
    }

}


