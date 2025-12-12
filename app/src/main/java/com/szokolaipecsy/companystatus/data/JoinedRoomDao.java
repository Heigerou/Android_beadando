package com.szokolaipecsy.companystatus.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

// Ez a DAO a JoinedRoom táblához tartozik
// Itt kezeljük, hogy egy szobához csatlakozott-e a felhasználó
@Dao
public interface JoinedRoomDao {

    // Lekérdezzük, hogy létezik-e már ilyen roomId az adatbázisban
    // Ha van, akkor a felhasználó már csatlakozott a szobához
    @Query("SELECT * FROM JoinedRoom WHERE roomId = :roomId LIMIT 1")
    JoinedRoom find(int roomId);

    // Beszúrás vagy frissítés
    // REPLACE: ha már létezik ilyen roomId, felülírja
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(JoinedRoom jr);
}
