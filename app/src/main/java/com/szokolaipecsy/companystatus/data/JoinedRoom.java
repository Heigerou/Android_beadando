package com.szokolaipecsy.companystatus.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

// Ez az Entity azt tárolja, hogy mely szobákhoz csatlakozott a felhasználó
// Nem a szoba adatait, csak azt, hogy "belépett-e" és milyen jelszóval
@Entity
public class JoinedRoom {

    // A szoba azonosítója
    // Ez az elsődleges kulcs
    @PrimaryKey
    public int roomId;

    // A szoba jelszavának hash-e
    // Ha a szoba védett, itt tároljuk a hash-t
    public String passwordHash;
}
