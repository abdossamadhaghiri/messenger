package org.example.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@Getter
@SuperBuilder
@EqualsAndHashCode(callSuper = false)
@ToString
public class PvModel extends ChatModel {

    private String first;
    private String second;

}
