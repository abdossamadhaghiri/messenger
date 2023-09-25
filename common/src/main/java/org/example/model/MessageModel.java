package org.example.model;

import lombok.*;

@AllArgsConstructor
@RequiredArgsConstructor
@NoArgsConstructor
@ToString
public class MessageModel {

    private @Getter Long id;

    private @NonNull @Getter @Setter String text;

    private @NonNull @Getter String sender;

    private @NonNull @Getter Long chatId;


}
