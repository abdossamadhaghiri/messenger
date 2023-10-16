package com.example.server.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.model.ChatModel;
import org.example.model.GroupModel;
import org.example.model.PvModel;
import org.example.model.UserModel;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.ArrayList;
import java.util.List;


@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    private String username;
    private String token;

    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn
    @ManyToMany
    private List<Pv> pvs;

    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn
    @ManyToMany
    private List<Group> groups;

    public UserModel toUserModel() {
        List<PvModel> pvModels = new ArrayList<>();
        List<GroupModel> groupModels = new ArrayList<>();
        this.pvs.forEach(pv -> pvModels.add(pv.toPvModel()));
        this.groups.forEach(group -> groupModels.add(group.toGroupModel()));
        return new UserModel(this.username, this.token, pvModels, groupModels);
    }

}
