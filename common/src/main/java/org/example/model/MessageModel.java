package org.example.model;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;

@AllArgsConstructor
@NoArgsConstructor(force = true)
@Value
public class MessageModel {

    Long id;

    String text;

    String sender;

    Long chatId;


}
