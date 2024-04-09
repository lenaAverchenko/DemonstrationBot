package com.example.demobot.entity;

import javax.persistence.*;
import lombok.Data;

import java.sql.Timestamp;

@Entity(name = "userDataTable")
@Data
public class TgUser {

    @Id
    private long chatId;

    private String firstName;
    private String lastName;
    private String userName;
    private Timestamp registeredAt;


}

