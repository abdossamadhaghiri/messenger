package org.example.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@Getter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class PvModel extends ChatModel {

    private String first;
    private String second;
    @Builder.Default
    private List<PvMessageModel> pvMessages = new ArrayList<>();

}
