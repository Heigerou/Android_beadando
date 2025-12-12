package com.szokolaipecsy.companystatus.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

// Ez a DAO a kedvenc szobák adatbázis műveleteit kezeli
// Itt mondjuk meg, hogyan lehet adatot lekérni, beszúrni vagy törölni
@Dao
public interface FavoriteDao {

    // Lekéri az összes kedvenc szoba ID-ját
    // LiveData -> ha változik az adat, a UI automatikusan frissül
    @Query("SELECT id FROM favorite_room")
    LiveData<List<Integer>> getAllIdsLive();

    // Kedvenc hozzáadása vagy frissítése
    // REPLACE: ha már létezik ilyen ID, akkor felülírja
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(FavoriteRoom item);

    // Kedvenc törlése ID alapján
    @Query("DELETE FROM favorite_room WHERE id = :id")
    void deleteById(int id);
}
