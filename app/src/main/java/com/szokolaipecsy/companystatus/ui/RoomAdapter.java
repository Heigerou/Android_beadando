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

public class RoomAdapter extends RecyclerView.Adapter<RoomAdapter.VH> {

    public static class RoomItem {
        private String id;
        private String name;
        private String subtitle;

        public RoomItem(String id, String name, String subtitle) {
            this.id = id;
            this.name = name;
            this.subtitle = subtitle;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getSubtitle() { return subtitle; }
    }

    public interface OnItemClick { void onItemClick(RoomItem item); }
    public interface OnFavClick { void onFavClick(RoomItem item, boolean currentlyFavorite); }

    private final ArrayList<RoomItem> items = new ArrayList<>();
    private boolean compact;
    private final OnItemClick onItemClick;
    private final OnFavClick onFavClick;

    private final Set<Integer> favoriteIds = new HashSet<>();

    public RoomAdapter(List<RoomItem> initial, boolean compact,
                       OnItemClick onItemClick, OnFavClick onFavClick) {
        if (initial != null) this.items.addAll(initial);
        this.compact = compact;
        this.onItemClick = onItemClick;
        this.onFavClick = onFavClick;
    }

    public void setFavoriteIds(Set<Integer> favIds) {
        favoriteIds.clear();
        if (favIds != null) favoriteIds.addAll(favIds);
        notifyDataSetChanged();
    }

    public void replaceData(List<RoomItem> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    public void setCompact(boolean compact) {
        this.compact = compact;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_room, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        RoomItem item = items.get(position);

        h.title.setText(item.getName());
        h.subtitle.setText(item.getSubtitle());

        int pad = (int) (h.itemView.getResources().getDisplayMetrics().density * (compact ? 8 : 16));
        h.itemView.setPadding(pad, pad, pad, pad);

        int idInt;
        try {
            idInt = Integer.parseInt(item.getId());
        } catch (NumberFormatException e) {
            idInt = -1;
        }

        boolean isFav = favoriteIds.contains(idInt);
        h.fav.setImageResource(isFav ? R.drawable.ic_star_24 : R.drawable.ic_star_border_24);

        h.itemView.setOnClickListener(v -> {
            if (onItemClick != null) onItemClick.onItemClick(item);
        });

        final int finalIdInt = idInt;
        h.fav.setOnClickListener(v -> {
            int pos = h.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;

            boolean currentlyFav = favoriteIds.contains(finalIdInt);

            if (currentlyFav) favoriteIds.remove(finalIdInt);
            else favoriteIds.add(finalIdInt);
            notifyItemChanged(pos);

            if (onFavClick != null) onFavClick.onFavClick(item, currentlyFav);
        });
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView title, subtitle;
        ImageView fav;
        VH(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.room_title);
            subtitle = itemView.findViewById(R.id.room_subtitle);
            fav = itemView.findViewById(R.id.iv_fav);
        }
    }
}
