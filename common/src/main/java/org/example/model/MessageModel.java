package org.example.model;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.Getter;
import lombok.NonNull;

@AllArgsConstructor
@RequiredArgsConstructor
@NoArgsConstructor
@ToString
public class MessageModel {

    private @Getter Long id;

    private @NonNull @Getter String text;

    private @NonNull @Getter String sender;

    private @NonNull @Getter Long chatId;


}
