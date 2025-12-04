package com.szokolaipecsy.companystatus.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface UserProfileDao {

    @Query("SELECT * FROM user_profile WHERE user_id = :userId LIMIT 1")
    LiveData<UserProfile> observeByUserId(String userId);

    @Query("SELECT * FROM user_profile WHERE user_id = :userId LIMIT 1")
    UserProfile getSync(String userId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(UserProfile profile);

    @Update
    void update(UserProfile profile);
}
