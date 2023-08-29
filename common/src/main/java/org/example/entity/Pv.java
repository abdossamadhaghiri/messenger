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


}
