package com.szokolaipecsy.companystatus.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(
        entities = {
                UserProfile.class,
                FavoriteRoom.class,
                JoinedRoom.class,
                MembershipEntity.class
        },
        version = 7,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    public static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private static volatile AppDatabase INSTANCE;

    public abstract UserProfileDao userProfileDao();
    public abstract FavoriteDao favoriteDao();
    public abstract JoinedRoomDao joinedRoomDao();
    public abstract MembershipDao membershipDao();

    public static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS memberships (" +
                            "id TEXT NOT NULL PRIMARY KEY, " +
                            "roomId TEXT NOT NULL, " +
                            "userId TEXT NOT NULL, " +
                            "role TEXT NOT NULL, " +
                            "joinedAt TEXT, " +
                            "lastActiveAt TEXT)"
            );
        }
    };
    public static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            try {
                db.execSQL("ALTER TABLE memberships ADD COLUMN userName TEXT");
                db.execSQL("ALTER TABLE memberships ADD COLUMN statusText TEXT");
                db.execSQL("ALTER TABLE memberships ADD COLUMN availability TEXT");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };
    public static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            try {
                db.execSQL("ALTER TABLE user_profile ADD COLUMN display_name TEXT");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "app.db"
                            )
                            .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
