package com.szokolaipecsy.companystatus.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.szokolaipecsy.companystatus.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Ez a RoomAdapter
// A HomeFragment RecyclerView-jában ez rajzolja ki a szobákat (név, alcím, kedvenc csillag)
public class RoomAdapter extends RecyclerView.Adapter<RoomAdapter.VH> {

    // Egy sor adata (egy szoba)
    public static class RoomItem {
        private String id;
        private String name;
        private String subtitle;

        // Konstruktor: id + név + alcím
        public RoomItem(String id, String name, String subtitle) {
            this.id = id;
            this.name = name;
            this.subtitle = subtitle;
        }

        // Getterek
        public String getId() { return id; }
        public String getName() { return name; }
        public String getSubtitle() { return subtitle; }
    }

    // Kattintás a teljes sorra (szoba megnyitása)
    public interface OnItemClick { void onItemClick(RoomItem item); }

    // Kattintás a kedvenc csillagra
    // currentlyFavorite = jelenleg kedvenc-e (a kattintás pillanatában)
    public interface OnFavClick { void onFavClick(RoomItem item, boolean currentlyFavorite); }

    // A lista elemei (szobák)
    private final ArrayList<RoomItem> items = new ArrayList<>();

    // Compact mód: kisebb padding (beállításból jön)
    private boolean compact;

    // Callbackek (HomeFragment adja át)
    private final OnItemClick onItemClick;
    private final OnFavClick onFavClick;

    // Kedvenc szobák ID-i (int-ben tároljuk, mert a DB is így kezeli)
    private final Set<Integer> favoriteIds = new HashSet<>();

    // Adapter konstruktor
    // initial: kezdeti lista
    // compact: compact mód
    // onItemClick: sor kattintás
    // onFavClick: csillag kattintás
    public RoomAdapter(List<RoomItem> initial, boolean compact,
                       OnItemClick onItemClick, OnFavClick onFavClick) {
        if (initial != null) this.items.addAll(initial);
        this.compact = compact;
        this.onItemClick = onItemClick;
        this.onFavClick = onFavClick;
    }

    // Kedvencek ID lista frissítése (HomeViewModel/DB után)
    public void setFavoriteIds(Set<Integer> favIds) {
        favoriteIds.clear();
        if (favIds != null) favoriteIds.addAll(favIds);

        // újrarajzolás, hogy a csillag ikon jó legyen
        notifyDataSetChanged();
    }

    // Teljes lista cseréje (pl. keresés/szűrés után)
    public void replaceData(List<RoomItem> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    // Compact mód beállítása (Settingsből jöhet)
    public void setCompact(boolean compact) {
        this.compact = compact;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        // Egy sor layout-ja: row_room
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_room, parent, false);

        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {

        // aktuális szoba
        RoomItem item = items.get(position);

        // név + alcím kiírása
        h.title.setText(item.getName());
        h.subtitle.setText(item.getSubtitle());

        // padding beállítása a compact mód alapján
        int pad = (int) (h.itemView.getResources().getDisplayMetrics().density * (compact ? 8 : 16));
        h.itemView.setPadding(pad, pad, pad, pad);

        // id string -> int (a kedvencek Set int-et tárol)
        int idInt;
        try {
            idInt = Integer.parseInt(item.getId());
        } catch (NumberFormatException e) {
            idInt = -1;
        }

        // Megnézzük kedvenc-e
        boolean isFav = favoriteIds.contains(idInt);

        // Csillag ikon beállítása:
        // ha kedvenc -> tele csillag, ha nem -> üres csillag
        h.fav.setImageResource(isFav ? R.drawable.ic_star_24 : R.drawable.ic_star_border_24);

        // Sor kattintás: megnyitjuk a szobát (RoomDetail)
        h.itemView.setOnClickListener(v -> {
            if (onItemClick != null) onItemClick.onItemClick(item);
        });

        // A csillag kattintásnál kell a végleges idInt
        final int finalIdInt = idInt;

        // Csillag kattintás: kedvenc ki/be
        h.fav.setOnClickListener(v -> {

            // pozíció lekérése (biztonság)
            int pos = h.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;

            // jelenlegi állapot
            boolean currentlyFav = favoriteIds.contains(finalIdInt);

            // lokálisan rögtön átállítjuk a Set-ben (gyors UI reakció)
            if (currentlyFav) favoriteIds.remove(finalIdInt);
            else favoriteIds.add(finalIdInt);

            // csak ezt az 1 sort frissítjük
            notifyItemChanged(pos);

            // szóljunk kifelé (HomeViewModel/DB), hogy ténylegesen mentse
            if (onFavClick != null) onFavClick.onFavClick(item, currentlyFav);
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    // ViewHolder: 1 sor UI elemei
    static class VH extends RecyclerView.ViewHolder {
        TextView title, subtitle;
        ImageView fav;

        VH(@NonNull View itemView) {
            super(itemView);

            // összekötés a row_room XML elemeivel
            title = itemView.findViewById(R.id.room_title);
            subtitle = itemView.findViewById(R.id.room_subtitle);
            fav = itemView.findViewById(R.id.iv_fav);
        }
    }
}
