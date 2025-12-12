package com.szokolaipecsy.companystatus.auth;

import android.content.Context;
import android.content.SharedPreferences;

// Ez az osztály a bejelentkezést kezeli
// Itt jegyezzük meg, hogy ki van belépve az appba
public final class AuthManager {

    // Singleton: az appban csak egy AuthManager van
    private static volatile AuthManager INSTANCE;

    // Innen kérjük le mindenhol az AuthManager-t
    public static AuthManager getInstance(Context ctx) {
        if (INSTANCE == null) {
            synchronized (AuthManager.class) { // ne jöjjön létre több példány
                if (INSTANCE == null) {
                    // ApplicationContextet használunk, hogy ne legyen gond később
                    INSTANCE = new AuthManager(ctx.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    // Ebben tároljuk el a belépési adatokat
    private final SharedPreferences sp;

    // Privát konstruktor, mert ez singleton
    private AuthManager(Context ctx) {
        // auth_prefs = a fájl neve, ahová mentünk
        sp = ctx.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
    }

    // Sikeres belépés után hívjuk meg
    // Elmentjük a user adatait
    public void save(String id, String name, String email, boolean stay) {
        sp.edit()
                .putString("id", id)       // felhasználó azonosító
                .putString("name", name)   // felhasználó neve
                .putString("email", email) // email cím
                .putBoolean("stay", stay)  // maradjon-e belépve
                .apply();                  // mentés
    }

    // Megnézzük, hogy be van-e lépve a felhasználó
    public boolean isLoggedIn() {
        // ha van userId, akkor be van lépve
        return getUserId() != null;
    }

    // Megnézzük, hogy automatikusan belépjen-e az app indításakor
    public boolean shouldAutoLogin() {
        return sp.getBoolean("stay", false);
    }

    // Kijelentkezés
    // Minden mentett adat törlődik
    public void logout() {
        sp.edit().clear().apply();
    }

    // Ezekkel kérjük le az elmentett adatokat

    public String getUserId() {
        return sp.getString("id", null);
    }

    public String getUserName() {
        return sp.getString("name", "");
    }

    public String getEmail() {
        return sp.getString("email", "");
    }
}
