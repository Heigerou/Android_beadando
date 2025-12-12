package com.szokolaipecsy.companystatus.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.szokolaipecsy.companystatus.data.FavoritesRepository;

import java.util.Set;

// Ez a HomeViewModel
// A HomeFragment és az adatbázis (kedvencek) között helyezkedik el
// Nem UI logika van itt, hanem adatok kezelése
public class HomeViewModel extends AndroidViewModel {

    // Repository, ami a kedvenceket kezeli (Room DB)
    private final FavoritesRepository repo;

    // LiveData, ami a kedvenc szobák ID-it tartalmazza
    // Ha változik, a UI automatikusan frissül
    private final LiveData<Set<Integer>> favoriteIds;

    // Konstruktor
    // AndroidViewModel -> kell az Application context
    public HomeViewModel(@NonNull Application app) {
        super(app);

        // Repository példány
        repo = FavoritesRepository.get(app);

        // Kedvencek figyelése LiveData-val
        favoriteIds = repo.favoriteIdsLive();
    }

    // A HomeFragment innen kéri le a kedvenc ID-ket
    public LiveData<Set<Integer>> getFavoriteIds() {
        return favoriteIds;
    }

    // Kedvenc ki/be kapcsolása
    // id = szoba ID
    // isFav = jelenlegi állapot
    // name, subtitle = mentjük a DB-be, hogy meg legyenek az adatok
    public void toggleFavorite(int id, boolean isFav, String name, String subtitle) {
        repo.toggle(id, isFav, name, subtitle);
    }
}
