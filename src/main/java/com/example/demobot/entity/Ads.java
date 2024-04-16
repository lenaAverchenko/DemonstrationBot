package com.example.demobot.entity;

import lombok.Data;
import javax.persistence.*;

@Data
@Entity(name = "adsTable")
public class Ads {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;
    private String ad;




}
