package com.szokolaipecsy.companystatus.ui;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.szokolaipecsy.companystatus.data.FavoritesRepository;
import java.util.Set;

public class HomeViewModel extends AndroidViewModel {
    private final FavoritesRepository repo;
    private final LiveData<Set<Integer>> favoriteIds;
    public HomeViewModel(@NonNull Application app) {
        super(app);
        repo = FavoritesRepository.get(app);
        favoriteIds = repo.favoriteIdsLive();
    }
    public LiveData<Set<Integer>> getFavoriteIds() {
        return favoriteIds;
    }
    public void toggleFavorite(int id, boolean isFav, String name, String subtitle) {
        repo.toggle(id, isFav, name, subtitle);
    }
}
