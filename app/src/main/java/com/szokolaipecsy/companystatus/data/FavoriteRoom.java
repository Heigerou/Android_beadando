package com.szokolaipecsy.companystatus.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

// Ez az osztály egy adatbázis tábla (Room Entity)
// Egy kedvenc szobát ír le
@Entity(tableName = "favorite_room")
public class FavoriteRoom {

    // Elsődleges kulcs
    // Ez alapján azonosítjuk a kedvenc szobát
    @PrimaryKey
    public int id;

    // A szoba neve
    // @NonNull -> nem lehet null, mindig kell legyen értéke
    @NonNull
    public String name;

    // A szoba alcíme (opcionális)
    public String subtitle;

    // Mikor frissült utoljára
    // pl. rendezéshez vagy összehasonlításhoz
    public long updatedAt;

    // Konstruktor
    // Amikor kedvencet mentünk az adatbázisba, ezzel hozzuk létre az objektumot
    public FavoriteRoom(int id, @NonNull String name, String subtitle, long updatedAt) {
        this.id = id;
        this.name = name;
        this.subtitle = subtitle;
        this.updatedAt = updatedAt;
    }
}
