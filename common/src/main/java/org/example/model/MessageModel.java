package org.example.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@AllArgsConstructor
@RequiredArgsConstructor
@NoArgsConstructor
@Data
public class MessageModel {

    private Long id;

    private @NonNull String text;

    private @NonNull String sender;

    private @NonNull Long chatId;


}
