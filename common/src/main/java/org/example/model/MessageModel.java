package org.example.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@RequiredArgsConstructor
@NoArgsConstructor
@Data
public class MessageModel {

    @Setter(AccessLevel.NONE)
    private Long id;

    private @NonNull String text;

    @Setter(AccessLevel.NONE)
    private @NonNull String sender;

    @Setter(AccessLevel.NONE)
    private @NonNull Long chatId;


}
