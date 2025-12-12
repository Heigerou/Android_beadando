package com.szokolaipecsy.companystatus.data;

import android.content.Context;

// Ez egy kis segédosztály az adatbázishoz
// Az a célja, hogy egyszerűbb legyen lekérni az AppDatabase-t
public final class DbProvider {

    // Privát konstruktor
    // Nem akarunk példányt létrehozni ebből az osztályból
    private DbProvider() { /* no instances */ }

    // Ezzel a metódussal kérjük le az adatbázist bárhonnan
    public static AppDatabase get(Context ctx) {
        // ApplicationContextet adunk át,
        // hogy ne legyen gond Activity élettartammal
        return AppDatabase.getInstance(ctx.getApplicationContext());
    }
}
