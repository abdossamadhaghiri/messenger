package org.example.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Value;

@AllArgsConstructor
@NoArgsConstructor(force = true)
@Value
@Builder(toBuilder = true)
public class MessageModel {

    Long id;

    String text;

    String sender;

    Long chatId;


}
