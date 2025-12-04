package com.szokolaipecsy.companystatus.data;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FavoritesRepository {

    private static FavoritesRepository INSTANCE;
    private final FavoriteDao favoriteDao;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    private FavoritesRepository(Context ctx) {
        AppDatabase db = AppDatabase.getInstance(ctx);
        this.favoriteDao = db.favoriteDao();
    }

    public static FavoritesRepository get(Context ctx) {
        if (INSTANCE == null) {
            synchronized (FavoritesRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new FavoritesRepository(ctx.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    public LiveData<Set<Integer>> favoriteIdsLive() {
        return Transformations.map(favoriteDao.getAllIdsLive(), list -> {
            Set<Integer> set = new HashSet<>();
            if (list != null) set.addAll(list);
            return set;
        });
    }

    public void addFavorite(int id, String name, String subtitle) {
        io.execute(() -> favoriteDao.upsert(
                new FavoriteRoom(id, name == null ? "" : name, subtitle, System.currentTimeMillis())));
    }

    public void removeFavorite(int id) {
        io.execute(() -> favoriteDao.deleteById(id));
    }

    public void toggle(int id, boolean isFav, String name, String subtitle) {
        if (isFav) removeFavorite(id);
        else addFavorite(id, name, subtitle);
    }
}
