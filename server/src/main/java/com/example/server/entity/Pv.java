package com.example.server.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.example.model.PvMessageModel;
import org.example.model.PvModel;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Data
@Entity
@SuperBuilder
public class Pv extends Chat {

    private String first;
    private String second;

    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn
    @OneToMany(cascade = CascadeType.ALL)
    @Builder.Default
    private List<PvMessage> pvMessages = new ArrayList<>();


    public PvModel toPvModel() {
        List<PvMessageModel> pvMessageModels = new ArrayList<>();
        this.pvMessages.forEach(pvMessage -> pvMessageModels.add(pvMessage.toPvMessageModel()));
        return PvModel.builder()
                .id(this.getId())
                .first(this.first)
                .second(this.second)
                .pvMessages(pvMessageModels)
                .build();
    }

    public static Pv fromPvModel(PvModel pvModel) {
        List<PvMessage> pvMessages = new ArrayList<>();
        pvModel.getPvMessages().forEach(pvMessageModel -> pvMessages.add(PvMessage.fromPvMessageModel(pvMessageModel)));
        return Pv.builder()
                .id(pvModel.getId())
                .first(pvModel.getFirst())
                .second(pvModel.getSecond())
                .pvMessages(pvMessages)
                .build();
    }
}
