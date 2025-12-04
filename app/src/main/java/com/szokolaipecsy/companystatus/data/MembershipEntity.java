package com.szokolaipecsy.companystatus.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "memberships")
public class MembershipEntity {

    @PrimaryKey
    @NonNull
    public String id;

    @NonNull
    public String roomId;

    @NonNull
    public String userId;

    @NonNull
    public String role;

    public String joinedAt;
    public String lastActiveAt;

    public String userName;

    public String statusText;

    public String availability;
}


