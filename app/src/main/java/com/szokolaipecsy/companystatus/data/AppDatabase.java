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

// Ez az app helyi adatbázisa (Room)
// Itt tároljuk azokat az adatokat, amiket nem akarunk mindig netről lekérni
@Database(
        entities = {
                UserProfile.class,     // felhasználó profil adatai
                FavoriteRoom.class,    // kedvenc szobák
                JoinedRoom.class,      // csatlakozott szobák
                MembershipEntity.class// szobák tagjai + státusz
        },
        version = 7,                 // adatbázis verzió
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    // Egy külön háttérszál az adatbázis műveletekhez
    // azért kell, mert adatbázist nem szabad UI szálon futtatni
    public static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    // Singleton példány – csak egy adatbázis legyen az appban
    private static volatile AppDatabase INSTANCE;

    // DAO-k: ezekkel érjük el az egyes táblákat
    public abstract UserProfileDao userProfileDao();
    public abstract FavoriteDao favoriteDao();
    public abstract JoinedRoomDao joinedRoomDao();
    public abstract MembershipDao membershipDao();

    // MIGRATION 4 -> 5
    // új memberships tábla létrehozása (szoba tagok)
    public static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS memberships (" +
                            "id TEXT NOT NULL PRIMARY KEY, " +   // tagság azonosító
                            "roomId TEXT NOT NULL, " +           // melyik szobához tartozik
                            "userId TEXT NOT NULL, " +           // melyik felhasználó
                            "role TEXT NOT NULL, " +             // szerep (pl. member)
                            "joinedAt TEXT, " +                  // mikor csatlakozott
                            "lastActiveAt TEXT)"                 // mikor volt aktív utoljára
            );
        }
    };

    // MIGRATION 5 -> 6
    // új mezők hozzáadása a memberships táblához
    public static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            try {
                db.execSQL("ALTER TABLE memberships ADD COLUMN userName TEXT");      // felhasználó neve
                db.execSQL("ALTER TABLE memberships ADD COLUMN statusText TEXT");    // státusz szöveg
                db.execSQL("ALTER TABLE memberships ADD COLUMN availability TEXT"); // elérhetőség
            } catch (Exception e) {
                // ha már létezik, ne dőljön el az app
                e.printStackTrace();
            }
        }
    };

    // MIGRATION 6 -> 7
    // új mező a user_profile táblához
    public static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            try {
                db.execSQL("ALTER TABLE user_profile ADD COLUMN display_name TEXT");
            } catch (Exception e) {
                // hiba esetén csak kiírjuk, az app megy tovább
                e.printStackTrace();
            }
        }
    };

    // Singleton adatbázis lekérése
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) { // egyszerre csak egy példány jöjjön létre
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(), // application context
                                    AppDatabase.class,                // adatbázis osztály
                                    "app.db"                          // adatbázis fájl neve
                            )
                            // migrációk hozzáadása, hogy frissítéskor ne vesszenek el adatok
                            .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
