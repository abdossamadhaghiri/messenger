package org.example.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@RequiredArgsConstructor
@ToString
@Entity
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private @Getter @Setter int id;

    private @NonNull @Getter @Setter String text;

    private @NonNull @Getter String sender;

    private @NonNull @Getter Long chatId;

    private @NonNull @Getter Long repliedMessageId;

    private @Getter @Setter String forwardedFrom;

}


