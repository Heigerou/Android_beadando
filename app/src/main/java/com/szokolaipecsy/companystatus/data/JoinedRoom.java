package com.szokolaipecsy.companystatus.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class JoinedRoom {
    @PrimaryKey public int roomId;
    public String passwordHash;
}
