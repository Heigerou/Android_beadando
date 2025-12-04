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

public class HomeFragment extends Fragment {

    private boolean showSnackOnUpdate = false;
    private RecyclerView recycler;
    private SwipeRefreshLayout swipeRefresh;
    private RoomAdapter adapter;
    private final List<RoomAdapter.RoomItem> allItems = new ArrayList<>();
    private boolean compact, showDividers;
    private HomeViewModel vm;
    private String currentQuery = "";
    private boolean onlyFavorites = false;
    private Set<Integer> latestFavoriteIds = new HashSet<>();
    private SwitchMaterial swOnlyFav;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        vm = new ViewModelProvider(this).get(HomeViewModel.class);

        recycler = view.findViewById(R.id.recycler_rooms);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));

        swipeRefresh = view.findViewById(R.id.swipe_refresh);

        swipeRefresh.setOnRefreshListener(() -> {
            showSnackOnUpdate = true;
            fetchRoomsFromApi();
        });

        SharedPreferences prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        compact = prefs.getBoolean("pref_compact_list", false);
        showDividers = prefs.getBoolean("pref_show_dividers", true);

        allItems.clear();

        adapter = new RoomAdapter(
                allItems,
                compact,
                    item -> {
                        Bundle args = new Bundle();
                        String roomId = item.getId();
                        args.putString("roomId", roomId);
                        NavHostFragment.findNavController(HomeFragment.this)
                                .navigate(R.id.action_homeFragment_to_roomDetailFragment, args);
                    },

                (item, isFav) -> {
                    vm.toggleFavorite(parseId(item.getId()), isFav, item.getName(), item.getSubtitle());
                    Snackbar.make(view,
                            isFav ? "Removed from favorites" : "Added to favorites",
                            Snackbar.LENGTH_SHORT).show();
                }
        );
        recycler.setAdapter(adapter);

        if (showDividers) {
            recycler.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
        }

        swOnlyFav = view.findViewById(R.id.switch_only_fav);
        swOnlyFav.setOnCheckedChangeListener((buttonView, isChecked) -> {
            onlyFavorites = isChecked;
            applyFilter();
        });

        SearchView search = view.findViewById(R.id.search_rooms);
        search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { return false; }
            @Override public boolean onQueryTextChange(String newText) {
                currentQuery = newText == null ? "" : newText.trim();
                applyFilter();
                return true;
            }
        });

        vm.getFavoriteIds().observe(getViewLifecycleOwner(), (Set<Integer> favIds) -> {
            latestFavoriteIds = favIds == null ? new HashSet<>() : new HashSet<>(favIds);
            adapter.setFavoriteIds(latestFavoriteIds);
            if (onlyFavorites) applyFilter();
        });

        Set<Integer> initialFavs = vm.getFavoriteIds().getValue();
        if (initialFavs != null) {
            latestFavoriteIds = new HashSet<>(initialFavs);
            adapter.setFavoriteIds(latestFavoriteIds);
        }

        applyFilter();

        View fab = view.findViewById(R.id.fab_create_room);
        if (fab != null) {
            fab.setOnClickListener(v -> {
                NavDirections action =
                        HomeFragmentDirections.actionHomeFragmentToCreateRoomFragment();
                NavHostFragment.findNavController(HomeFragment.this).navigate(action);
            });
        }

        if (allItems.isEmpty()) {
            swipeRefresh.setRefreshing(true);
            showSnackOnUpdate = false;
            fetchRoomsFromApi();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!AuthManager.getInstance(requireContext()).isLoggedIn()) {
            NavHostFragment.findNavController(this)
                    .navigate(R.id.authGateFragment);
            return;
        }

        SharedPreferences prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        boolean newCompact = prefs.getBoolean("pref_compact_list", false);
        boolean newDividers = prefs.getBoolean("pref_show_dividers", true);

        if (newCompact != compact) {
            compact = newCompact;
            adapter.setCompact(compact);
        }

        if (newDividers != showDividers) {
            showDividers = newDividers;
            while (recycler.getItemDecorationCount() > 0) {
                recycler.removeItemDecorationAt(0);
            }
            if (showDividers) {
                recycler.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
            }
        }
    }

    private void applyFilter() {
        String q = currentQuery == null ? "" : currentQuery.toLowerCase();

        List<RoomAdapter.RoomItem> filtered = new ArrayList<>();
        for (RoomAdapter.RoomItem it : allItems) {
            boolean matchesQuery = q.isEmpty() || it.getName().toLowerCase().contains(q);
            boolean matchesFav = !onlyFavorites
                    || (latestFavoriteIds != null && latestFavoriteIds.contains(parseId(it.getId())));
            if (matchesQuery && matchesFav) filtered.add(it);
        }
        adapter.replaceData(filtered);

        View root = getView();
        if (root != null) {
            View empty = root.findViewById(R.id.empty_view);
            if (empty != null) empty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void fetchRoomsFromApi() {
        View root = getView();
        if (root != null) {
            View empty = root.findViewById(R.id.empty_view);
            if (empty != null) empty.setVisibility(View.GONE);
        }
        if (swipeRefresh != null) swipeRefresh.setRefreshing(true);

        ApiClient.rooms().getRooms().enqueue(new Callback<List<RoomDto>>() {
            @Override
            public void onResponse(Call<List<RoomDto>> call, Response<List<RoomDto>> resp) {
                if (!isAdded()) return;
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);

                View v = getView();
                if (!resp.isSuccessful() || resp.body() == null) {
                    if (v != null) Snackbar.make(v, "Failed to load rooms (" + resp.code() + ")", Snackbar.LENGTH_LONG).show();
                    showSnackOnUpdate = false; // reset
                    return;
                }

                allItems.clear();
                for (RoomDto d : resp.body()) {
                    allItems.add(new RoomAdapter.RoomItem(
                            d.id,
                            d.name != null ? d.name : "",
                            d.subtitle != null ? d.subtitle : ""
                    ));
                }

                applyFilter();

                if (v != null && showSnackOnUpdate) {
                    Snackbar.make(v, "Rooms updated: " + allItems.size(), Snackbar.LENGTH_LONG).show();
                }
                showSnackOnUpdate = false;
            }

            @Override
            public void onFailure(Call<List<RoomDto>> call, Throwable t) {
                if (!isAdded()) return;
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);

                View v = getView();
                if (v != null) {
                    Snackbar.make(v, "Network error: " + (t.getMessage() == null ? "unknown" : t.getMessage()),
                            Snackbar.LENGTH_LONG).show();
                }
                showSnackOnUpdate = false;
            }
        });
    }
    private int parseId(String id) {
        try {
            return Integer.parseInt(id);
        } catch (Exception e) {
            return -1;
        }
    }

}
