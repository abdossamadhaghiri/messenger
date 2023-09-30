package com.example.server.entity;

import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.example.model.ChatModel;
import org.example.model.PvModel;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
public class Pv extends Chat {

    private String first;

    private String second;

    public Pv(Long id, String first, String second) {
        this.setId(id);
        this.first = first;
        this.second = second;
    }

    @Override
    public ChatModel createChatModel() {
        return new PvModel(this.getId(), this.first, this.second);
    }
}
