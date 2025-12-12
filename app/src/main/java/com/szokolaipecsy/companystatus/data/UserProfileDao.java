package com.szokolaipecsy.companystatus.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

// Ez a DAO a user_profile táblát kezeli
// A felhasználó profil adatainak lekérésére és mentésére szolgál
@Dao
public interface UserProfileDao {

    // Profil lekérése LiveData-val
    // Ha a profil adat változik, a UI automatikusan frissül
    @Query("SELECT * FROM user_profile WHERE user_id = :userId LIMIT 1")
    LiveData<UserProfile> observeByUserId(String userId);

    // Profil lekérése szinkron módon
    // Ezt akkor használjuk, amikor háttérszálon gyorsan kell adat
    @Query("SELECT * FROM user_profile WHERE user_id = :userId LIMIT 1")
    UserProfile getSync(String userId);

    // Profil mentése vagy frissítése
    // REPLACE: ha már létezik, felülírja
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(UserProfile profile);

    // Profil frissítése
    // Akkor használjuk, ha már létezik az adat
    @Update
    void update(UserProfile profile);
}
