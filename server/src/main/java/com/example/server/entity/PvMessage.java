package com.example.server.entity;

import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.example.model.PvMessageModel;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@SuperBuilder
@NoArgsConstructor
public class PvMessage extends Message {

    private Long pvId;

    public PvMessageModel toPvMessageModel() {
        return PvMessageModel.builder()
                .id(this.getId())
                .text(this.getText())
                .sender(this.getSender())
                .pvId(this.pvId)
                .repliedMessageId(this.getRepliedMessageId())
                .forwardedFrom(this.getForwardedFrom())
                .build();
    }

    public static PvMessage fromPvMessageModel(PvMessageModel pvMessageModel) {
        return PvMessage.builder()
                .id(pvMessageModel.getId())
                .text(pvMessageModel.getText())
                .sender(pvMessageModel.getSender())
                .pvId(pvMessageModel.getPvId())
                .repliedMessageId(pvMessageModel.getRepliedMessageId())
                .forwardedFrom(pvMessageModel.getForwardedFrom())
                .build();
    }

}
