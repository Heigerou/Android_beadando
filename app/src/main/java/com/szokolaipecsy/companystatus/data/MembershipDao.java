package com.szokolaipecsy.companystatus.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Delete;

import java.util.List;

@Dao
public interface MembershipDao {

    @Query("SELECT * FROM memberships WHERE roomId = :roomId ORDER BY availability DESC, lastActiveAt DESC, userName ASC")
    LiveData<List<MembershipEntity>> observeByRoom(String roomId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertAll(List<MembershipEntity> items);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(MembershipEntity item);

    @Query("DELETE FROM memberships WHERE roomId = :roomId")
    void deleteAllByRoom(String roomId);

    @Query("SELECT * FROM memberships WHERE roomId = :roomId AND userId = :userId LIMIT 1")
    MembershipEntity findOne(String roomId, String userId);
    @Query("UPDATE memberships SET userName = :newName WHERE userId = :userId")
    void updateNameForUser(String userId, String newName);

    @Delete
    void delete(MembershipEntity item);
}
