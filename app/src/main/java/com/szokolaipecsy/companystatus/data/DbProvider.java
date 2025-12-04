package com.szokolaipecsy.companystatus.data;

import android.content.Context;

public final class DbProvider {

    private DbProvider() { /* no instances */ }

    public static AppDatabase get(Context ctx) {
        return AppDatabase.getInstance(ctx.getApplicationContext());
    }
}
