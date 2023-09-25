package org.example.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public class PvModel extends ChatModel {

    private @Getter String first;
    private @Getter String second;

    public PvModel(Long id, String first, String second) {
        this.setId(id);
        this.first = first;
        this.second = second;
    }
}
