package com.szokolaipecsy.companystatus.data;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Ez a Repository a kedvencek kezeléséért felel
// Itt van összefogva minden logika a FavoriteDao körül
public class FavoritesRepository {

    // Singleton példány – mindenhol ugyanazt használjuk
    private static FavoritesRepository INSTANCE;

    // DAO, amin keresztül az adatbázist elérjük
    private final FavoriteDao favoriteDao;

    // Háttérszál az adatbázis műveletekhez
    // mert adatbázist nem futtatunk UI szálon
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    // Privát konstruktor – singleton miatt
    private FavoritesRepository(Context ctx) {
        // Lekérjük az adatbázist
        AppDatabase db = AppDatabase.getInstance(ctx);
        // Lekérjük belőle a FavoriteDao-t
        this.favoriteDao = db.favoriteDao();
    }

    // Repository lekérése bárhonnan
    public static FavoritesRepository get(Context ctx) {
        if (INSTANCE == null) {
            synchronized (FavoritesRepository.class) { // egyszerre csak egy példány
                if (INSTANCE == null) {
                    INSTANCE = new FavoritesRepository(ctx.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    // LiveData, ami az összes kedvenc ID-t adja vissza
    // Listából Set-et csinálunk, hogy könnyebb legyen ellenőrizni
    // pl. egy szoba kedvenc-e vagy sem
    public LiveData<Set<Integer>> favoriteIdsLive() {
        return Transformations.map(favoriteDao.getAllIdsLive(), list -> {
            Set<Integer> set = new HashSet<>();
            if (list != null) set.addAll(list);
            return set;
        });
    }

    // Kedvenc hozzáadása
    public void addFavorite(int id, String name, String subtitle) {
        io.execute(() -> favoriteDao.upsert(
                // új FavoriteRoom objektum létrehozása
                // currentTimeMillis -> mikor lett utoljára frissítve
                new FavoriteRoom(
                        id,
                        name == null ? "" : name, // ha nincs név, legyen üres
                        subtitle,
                        System.currentTimeMillis()
                )
        ));
    }

    // Kedvenc törlése ID alapján
    public void removeFavorite(int id) {
        io.execute(() -> favoriteDao.deleteById(id));
    }

    // Kedvenc ki-be kapcsolása
    // ha már kedvenc -> töröljük
    // ha nem kedvenc -> hozzáadjuk
    public void toggle(int id, boolean isFav, String name, String subtitle) {
        if (isFav) removeFavorite(id);
        else addFavorite(id, name, subtitle);
    }
}
