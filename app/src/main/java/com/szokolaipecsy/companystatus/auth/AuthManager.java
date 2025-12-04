package com.szokolaipecsy.companystatus.auth;

import android.content.Context;
import android.content.SharedPreferences;

public final class AuthManager {

    private static volatile AuthManager INSTANCE;

    public static AuthManager getInstance(Context ctx) {
        if (INSTANCE == null) {
            synchronized (AuthManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new AuthManager(ctx.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    private final SharedPreferences sp;

    private AuthManager(Context ctx) {
        sp = ctx.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
    }

    public void save(String id, String name, String email, boolean stay) {
        sp.edit()
                .putString("id", id)
                .putString("name", name)
                .putString("email", email)
                .putBoolean("stay", stay)
                .apply();
    }

    public boolean isLoggedIn() {
        return getUserId() != null;
    }

    public boolean shouldAutoLogin() {
        return sp.getBoolean("stay", false);
    }

    public void logout() {
        sp.edit().clear().apply();
    }

    public String getUserId()   { return sp.getString("id",   null); }
    public String getUserName() { return sp.getString("name", ""); }
    public String getEmail()    { return sp.getString("email",""); }
}
