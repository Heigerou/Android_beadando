package com.szokolaipecsy.companystatus.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

// Ez az Entity a felhasználó profil adatait tárolja
// Nem login adat, hanem kiegészítő infók (telefon, céges email stb.)
@Entity(tableName = "user_profile")
public class UserProfile {

    // A felhasználó egyedi azonosítója
    // Ez a PRIMARY KEY
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "user_id")
    public String userId;

    // A felhasználó neve
    // Ezt pl. profil oldalon jelenítjük meg
    @ColumnInfo(name = "name")
    public String name;

    // Megjelenített név
    // Később külön kezelhető a sima névtől
    @ColumnInfo(name = "display_name")
    public String displayName;

    // Email cím
    @ColumnInfo(name = "email")
    public String email;

    // Privát (személyes) telefonszám
    @ColumnInfo(name = "personal_phone")
    public String personalPhone;

    // Céges telefonszám
    @ColumnInfo(name = "company_phone")
    public String companyPhone;

    // Belső céges email
    @ColumnInfo(name = "internal_email")
    public String internalEmail;

    // Üres konstruktor
    // Room-nak szüksége van rá
    public UserProfile() {}

    // Konstruktor, amikor kézzel hozunk létre UserProfile-t
    // Itt töltjük fel az alap adatokat
    public UserProfile(@NonNull String userId,
                       String name,
                       String email,
                       String personalPhone,
                       String companyPhone,
                       String internalEmail) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.personalPhone = personalPhone;
        this.companyPhone = companyPhone;
        this.internalEmail = internalEmail;

        // alapból nincs külön display név
        this.displayName = null;
    }
}
