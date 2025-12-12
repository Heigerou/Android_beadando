package com.szokolaipecsy.companystatus.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Delete;

import java.util.List;

// Ez a DAO a memberships táblát kezeli
// Itt vannak a szobák tagjai (ki van bent, milyen státusszal)
@Dao
public interface MembershipDao {

    // Egy adott szoba összes tagjának lekérése
    // LiveData -> ha változik a lista, a UI automatikusan frissül
    // Rendezés:
    // 1. availability (pl. elérhető legyen felül)
    // 2. lastActiveAt (aki most volt aktívabb, feljebb)
    // 3. userName (ABC sorrend)
    @Query("SELECT * FROM memberships WHERE roomId = :roomId ORDER BY availability DESC, lastActiveAt DESC, userName ASC")
    LiveData<List<MembershipEntity>> observeByRoom(String roomId);

    // Több membership mentése egyszerre
    // REPLACE -> ha már léteznek, felülírja őket
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertAll(List<MembershipEntity> items);

    // Egyetlen membership mentése vagy frissítése
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(MembershipEntity item);

    // Egy adott szoba összes tagjának törlése
    // pl. amikor újra szinkronizálunk szerverről
    @Query("DELETE FROM memberships WHERE roomId = :roomId")
    void deleteAllByRoom(String roomId);

    // Megkeresi, hogy egy adott user benne van-e egy szobában
    // LIMIT 1 -> egy user csak egyszer lehet bent
    @Query("SELECT * FROM memberships WHERE roomId = :roomId AND userId = :userId LIMIT 1")
    MembershipEntity findOne(String roomId, String userId);

    // Egy felhasználó nevének frissítése minden szobában
    // pl. ha a user megváltoztatja a megjelenített nevét
    @Query("UPDATE memberships SET userName = :newName WHERE userId = :userId")
    void updateNameForUser(String userId, String newName);

    // Egy konkrét membership törlése
    // pl. ha a felhasználó kilép a szobából
    @Delete
    void delete(MembershipEntity item);
}
