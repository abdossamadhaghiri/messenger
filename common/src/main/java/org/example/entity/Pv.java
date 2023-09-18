package org.example.entity;

import jakarta.persistence.Entity;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
public class Pv extends Chat {

    private @Getter @Setter String first;

    private @Getter @Setter String second;

    public Pv(Long id, String first, String second) {
        this.setId(id);
        this.first = first;
        this.second = second;
    }
}
