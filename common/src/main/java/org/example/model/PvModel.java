package org.example.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@Getter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class PvModel extends ChatModel {

    private String first;
    private String second;


}
