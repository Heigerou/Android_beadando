package com.szokolaipecsy.companystatus.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.szokolaipecsy.companystatus.R;
import com.szokolaipecsy.companystatus.network.ApiClient;
import com.szokolaipecsy.companystatus.network.RoomDto;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.szokolaipecsy.companystatus.auth.AuthManager;

// Ez a HomeFragment
// Itt jelenik meg a szobák listája (RecyclerView)
// Van kereső, frissítés (SwipeRefresh), kedvencek szűrés, és FAB új szoba létrehozásra
public class HomeFragment extends Fragment {

    // Ez azért kell, hogy ha lehúzom frissítésre, akkor írjon ki "Rooms updated" snackbart
    private boolean showSnackOnUpdate = false;

    // RecyclerView: szobák listája
    private RecyclerView recycler;

    // SwipeRefresh: lehúzós frissítés
    private SwipeRefreshLayout swipeRefresh;

    // Adapter: a RecyclerView-hoz
    private RoomAdapter adapter;

    // Az összes szoba itt van eltárolva (amit API-ról lekértünk)
    private final List<RoomAdapter.RoomItem> allItems = new ArrayList<>();

    // Beállítások: compact lista + elválasztó vonalak
    private boolean compact, showDividers;

    // ViewModel: itt van a kedvencek kezelése (pl. toggle)
    private HomeViewModel vm;

    // Kereső szövege
    private String currentQuery = "";

    // Csak kedvencek megjelenítése kapcsoló
    private boolean onlyFavorites = false;

    // Legfrissebb kedvenc ID-k (DB-ből jön)
    private Set<Integer> latestFavoriteIds = new HashSet<>();

    // Switch a UI-on (only favorites)
    private SwitchMaterial swOnlyFav;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // Betölti a fragment_home layoutot
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ViewModel példány
        vm = new ViewModelProvider(this).get(HomeViewModel.class);

        // RecyclerView beállítása
        recycler = view.findViewById(R.id.recycler_rooms);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));

        // SwipeRefresh beállítása
        swipeRefresh = view.findViewById(R.id.swipe_refresh);

        // Ha lehúzzuk frissítésre, újra lekérjük a szobákat
        swipeRefresh.setOnRefreshListener(() -> {
            showSnackOnUpdate = true;
            fetchRoomsFromApi();
        });

        // Beállítások beolvasása SharedPreferences-ből
        SharedPreferences prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        compact = prefs.getBoolean("pref_compact_list", false);
        showDividers = prefs.getBoolean("pref_show_dividers", true);

        // Lista alaphelyzetbe
        allItems.clear();

        // Adapter létrehozása:
        // 1) kattintás szobára -> átmegyünk a RoomDetail oldalra
        // 2) kedvenc katt -> toggle + üzenet
        adapter = new RoomAdapter(
                allItems,
                compact,

                // Ha rákattintok egy szobára
                item -> {
                    Bundle args = new Bundle();
                    String roomId = item.getId();
                    args.putString("roomId", roomId);

                    // Navigáció a részletező oldalra
                    NavHostFragment.findNavController(HomeFragment.this)
                            .navigate(R.id.action_homeFragment_to_roomDetailFragment, args);
                },

                // Kedvenc ki/be
                (item, isFav) -> {
                    // parseId: string id -> int id
                    vm.toggleFavorite(parseId(item.getId()), isFav, item.getName(), item.getSubtitle());

                    // kis visszajelzés
                    Snackbar.make(view,
                            isFav ? "Removed from favorites" : "Added to favorites",
                            Snackbar.LENGTH_SHORT).show();
                }
        );

        // Adapter ráadása a RecyclerView-ra
        recycler.setAdapter(adapter);

        // Elválasztó vonalak (ha be van kapcsolva)
        if (showDividers) {
            recycler.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
        }

        // "Only favorites" switch
        swOnlyFav = view.findViewById(R.id.switch_only_fav);
        swOnlyFav.setOnCheckedChangeListener((buttonView, isChecked) -> {
            onlyFavorites = isChecked;
            applyFilter(); // újraszűrjük a listát
        });

        // Keresőmező
        SearchView search = view.findViewById(R.id.search_rooms);
        search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            // Enterre most nem csinálunk külön dolgot
            @Override public boolean onQueryTextSubmit(String query) { return false; }

            // Gépelés közben szűrünk
            @Override public boolean onQueryTextChange(String newText) {
                currentQuery = newText == null ? "" : newText.trim();
                applyFilter();
                return true;
            }
        });

        // Kedvencek figyelése (LiveData)
        // Ha változik, frissítjük az adaptert és a szűrést
        vm.getFavoriteIds().observe(getViewLifecycleOwner(), (Set<Integer> favIds) -> {
            latestFavoriteIds = favIds == null ? new HashSet<>() : new HashSet<>(favIds);
            adapter.setFavoriteIds(latestFavoriteIds);
            if (onlyFavorites) applyFilter();
        });

        // Ha már van kezdeti value, akkor azzal is frissítünk
        Set<Integer> initialFavs = vm.getFavoriteIds().getValue();
        if (initialFavs != null) {
            latestFavoriteIds = new HashSet<>(initialFavs);
            adapter.setFavoriteIds(latestFavoriteIds);
        }

        // Alap szűrés alkalmazása (mielőtt jönnének adatok is)
        applyFilter();

        // FAB: új szoba létrehozás
        View fab = view.findViewById(R.id.fab_create_room);
        if (fab != null) {
            fab.setOnClickListener(v -> {
                NavDirections action =
                        HomeFragmentDirections.actionHomeFragmentToCreateRoomFragment();
                NavHostFragment.findNavController(HomeFragment.this).navigate(action);
            });
        }

        // Ha még nincs adat, akkor első betöltés: API hívás
        if (allItems.isEmpty()) {
            swipeRefresh.setRefreshing(true);
            showSnackOnUpdate = false;
            fetchRoomsFromApi();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Biztonság: ha nincs belépve, visszadob AuthGate-re
        if (!AuthManager.getInstance(requireContext()).isLoggedIn()) {
            NavHostFragment.findNavController(this)
                    .navigate(R.id.authGateFragment);
            return;
        }

        // Beállítások újraolvasása (ha a user a Settingsben átállította)
        SharedPreferences prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        boolean newCompact = prefs.getBoolean("pref_compact_list", false);
        boolean newDividers = prefs.getBoolean("pref_show_dividers", true);

        // Compact mód frissítése
        if (newCompact != compact) {
            compact = newCompact;
            adapter.setCompact(compact);
        }

        // Divider frissítése
        if (newDividers != showDividers) {
            showDividers = newDividers;

            // először minden dekorációt leszedünk
            while (recycler.getItemDecorationCount() > 0) {
                recycler.removeItemDecorationAt(0);
            }

            // ha kell, visszatesszük a vonalakat
            if (showDividers) {
                recycler.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
            }
        }
    }

    // Szűrés: kereső + only favorites
    private void applyFilter() {
        String q = currentQuery == null ? "" : currentQuery.toLowerCase();

        List<RoomAdapter.RoomItem> filtered = new ArrayList<>();

        for (RoomAdapter.RoomItem it : allItems) {

            // keresés név alapján
            boolean matchesQuery = q.isEmpty() || it.getName().toLowerCase().contains(q);

            // kedvenc szűrés (ha csak kedvenceket akarunk)
            boolean matchesFav = !onlyFavorites
                    || (latestFavoriteIds != null && latestFavoriteIds.contains(parseId(it.getId())));

            // ha mindkettő igaz, akkor benne marad
            if (matchesQuery && matchesFav) filtered.add(it);
        }

        // Adapter frissítése a szűrt listával
        adapter.replaceData(filtered);

        // Üres nézet kezelése (ha nincs találat)
        View root = getView();
        if (root != null) {
            View empty = root.findViewById(R.id.empty_view);
            if (empty != null) empty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    // Szobák lekérése API-ról
    private void fetchRoomsFromApi() {

        // üres nézetet elrejtjük, mert most töltünk
        View root = getView();
        if (root != null) {
            View empty = root.findViewById(R.id.empty_view);
            if (empty != null) empty.setVisibility(View.GONE);
        }

        // loading jelzés
        if (swipeRefresh != null) swipeRefresh.setRefreshing(true);

        // API hívás: getRooms
        ApiClient.rooms().getRooms().enqueue(new Callback<List<RoomDto>>() {

            @Override
            public void onResponse(Call<List<RoomDto>> call, Response<List<RoomDto>> resp) {

                // ha már nincs fragment, ne csináljunk semmit
                if (!isAdded()) return;

                // loading kikapcs
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);

                View v = getView();

                // ha hiba vagy nincs body
                if (!resp.isSuccessful() || resp.body() == null) {
                    if (v != null) Snackbar.make(v,
                            "Failed to load rooms (" + resp.code() + ")",
                            Snackbar.LENGTH_LONG).show();
                    showSnackOnUpdate = false;
                    return;
                }

                // új lista felépítése
                allItems.clear();
                for (RoomDto d : resp.body()) {
                    allItems.add(new RoomAdapter.RoomItem(
                            d.id,
                            d.name != null ? d.name : "",
                            d.subtitle != null ? d.subtitle : ""
                    ));
                }

                // szűrés alkalmazása friss adatokra
                applyFilter();

                // ha frissítésből jöttünk (pull-to-refresh), akkor írjuk ki
                if (v != null && showSnackOnUpdate) {
                    Snackbar.make(v,
                            "Rooms updated: " + allItems.size(),
                            Snackbar.LENGTH_LONG).show();
                }

                // visszaállítás
                showSnackOnUpdate = false;
            }

            @Override
            public void onFailure(Call<List<RoomDto>> call, Throwable t) {

                // ha már nincs fragment, ne csináljunk semmit
                if (!isAdded()) return;

                // loading kikapcs
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);

                // hálózati hiba üzenet
                View v = getView();
                if (v != null) {
                    Snackbar.make(v,
                            "Network error: " + (t.getMessage() == null ? "unknown" : t.getMessage()),
                            Snackbar.LENGTH_LONG).show();
                }

                showSnackOnUpdate = false;
            }
        });
    }

    // String ID -> int (kedvencekhez kell, mert ott int ID-k vannak)
    private int parseId(String id) {
        try {
            return Integer.parseInt(id);
        } catch (Exception e) {
            return -1;
        }
    }
}
