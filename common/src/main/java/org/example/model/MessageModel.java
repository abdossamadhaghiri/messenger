package org.example.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@Getter
@EqualsAndHashCode
public abstract class MessageModel {

    private Long id;

    private String text;

    private String sender;

    @Builder.Default
    private Long repliedMessageId = 0L;

    private String forwardedFrom;


}
