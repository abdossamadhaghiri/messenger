package org.example.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class UserModel {

    private String username;
    private String token;
    private List<PvModel> pvs;
    private List<GroupModel> groups;

}
