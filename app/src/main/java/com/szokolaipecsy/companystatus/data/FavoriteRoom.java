package com.szokolaipecsy.companystatus.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "favorite_room")
public class FavoriteRoom {
    @PrimaryKey
    public int id;

    @NonNull
    public String name;

    public String subtitle;
    public long updatedAt;

    public FavoriteRoom(int id, @NonNull String name, String subtitle, long updatedAt) {
        this.id = id;
        this.name = name;
        this.subtitle = subtitle;
        this.updatedAt = updatedAt;
    }
}
