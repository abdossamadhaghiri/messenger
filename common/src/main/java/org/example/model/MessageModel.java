package org.example.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@RequiredArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@EqualsAndHashCode
public class MessageModel {

    private Long id;

    private @NonNull @Setter String text;

    private @NonNull String sender;

    private @NonNull Long chatId;


}
