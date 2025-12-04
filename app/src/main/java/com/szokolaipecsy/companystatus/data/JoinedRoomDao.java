package com.szokolaipecsy.companystatus.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface JoinedRoomDao {
    @Query("SELECT * FROM JoinedRoom WHERE roomId = :roomId LIMIT 1")
    JoinedRoom find(int roomId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(JoinedRoom jr);
}
