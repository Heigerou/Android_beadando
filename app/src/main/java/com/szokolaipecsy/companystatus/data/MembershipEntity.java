package com.szokolaipecsy.companystatus.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

// Ez az Entity egy szoba–felhasználó kapcsolatot ír le
// Vagyis: ki van bent melyik szobában és milyen státusszal
@Entity(tableName = "memberships")
public class MembershipEntity {

    // A tagság egyedi azonosítója
    // Ez a PRIMARY KEY
    @PrimaryKey
    @NonNull
    public String id;

    // Annak a szobának az ID-ja, amelyhez tartozik
    @NonNull
    public String roomId;

    // A felhasználó ID-ja
    @NonNull
    public String userId;

    // A felhasználó szerepe a szobában
    // pl. member, admin
    @NonNull
    public String role;

    // Mikor csatlakozott a szobához
    public String joinedAt;

    // Mikor volt aktív utoljára
    // ezt használjuk a "last active" megjelenítéshez
    public String lastActiveAt;

    // A felhasználó megjelenített neve a szobában
    public String userName;

    // Egy rövid státusz szöveg
    // pl. "meetingen", "dolgozom", stb.
    public String statusText;

    // Elérhetőség állapota
    // pl. AVAILABLE / BUSY / AWAY
    public String availability;
}
