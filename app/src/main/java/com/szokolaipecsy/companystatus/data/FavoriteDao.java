package com.szokolaipecsy.companystatus.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface FavoriteDao {

    @Query("SELECT id FROM favorite_room")
    LiveData<List<Integer>> getAllIdsLive();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(FavoriteRoom item);

    @Query("DELETE FROM favorite_room WHERE id = :id")
    void deleteById(int id);
}
