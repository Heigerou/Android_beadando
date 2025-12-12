package com.szokolaipecsy.companystatus.ui;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.szokolaipecsy.companystatus.R;
import com.szokolaipecsy.companystatus.data.MembershipEntity;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

// Ez a MemberAdapter
// RecyclerView adapter, ami a szoba tagjait jeleníti meg (név, státusz, last active, elérhetőség pötty)
public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.MemberVH> {

    // Kattintás esemény a tagokra (pl. profil megnyitás)
    public interface OnMemberClick {
        void onClick(String userId);
    }

    // A tagok listája (MembershipEntity-k)
    private final List<MembershipEntity> data = new ArrayList<>();

    // Kattintás callback
    private final OnMemberClick click;

    // Saját user adatok (hogy ki tudjuk írni: (You))
    private String myUserId = null;
    private String myUserName = null;

    // Saját profil beállítása az adapternek
    // pl. ha a user nevét külön helyről tudjuk, itt átadjuk
    public void setMyProfile(String userId, String name) {
        this.myUserId = userId;
        this.myUserName = (name != null && !name.isEmpty()) ? name : null;

        // újrarajzolás
        notifyDataSetChanged();
    }

    // Konstruktor: kötelező a kattintás kezelő
    public MemberAdapter(@NonNull OnMemberClick click) {
        this.click = click;
    }

    // Lista frissítése
    public void submit(List<MembershipEntity> items) {
        data.clear();
        if (items != null) data.addAll(items);

        // Ha megvan a saját userId + saját név, akkor a saját rekordunk nevét felülírjuk
        // (hogy biztosan a helyi profilnév jelenjen meg)
        if (myUserId != null && myUserName != null) {
            for (MembershipEntity m : data) {
                if (m.userId.equals(myUserId)) {
                    m.userName = myUserName;
                }
            }
        }

        // Saját user-t felhozzuk a lista elejére
        // így mindig felül látom magamat
        if (myUserId != null) {
            MembershipEntity mine = null;
            int index = -1;

            for (int i = 0; i < data.size(); i++) {
                MembershipEntity m = data.get(i);
                if (myUserId.equals(m.userId)) {
                    mine = m;
                    index = i;
                    break;
                }
            }

            // ha megtaláltuk és nem az első, akkor áttesszük 0. helyre
            if (mine != null && index > 0) {
                data.remove(index);
                data.add(0, mine);
            }
        }

        // adapter frissítés
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MemberVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        // item_member layout felfújása (1 sor a listában)
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_member, parent, false);

        return new MemberVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberVH holder, int position) {

        // aktuális membership
        MembershipEntity m = data.get(position);

        // sor feltöltése adatokkal
        holder.bind(m, myUserId);

        // kattintás: átadjuk a userId-t
        holder.itemView.setOnClickListener(v -> {
            if (click != null) click.onClick(m.userId);
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    // ViewHolder: 1 darab sor (member) UI elemei
    static class MemberVH extends RecyclerView.ViewHolder {

        final TextView tvName;
        final TextView tvStatusText;
        final TextView tvLastActive;
        final TextView tvAvailabilityDot;

        MemberVH(@NonNull View itemView) {
            super(itemView);

            // összekötés az XML elemekkel
            tvName            = itemView.findViewById(R.id.tvName);
            tvStatusText      = itemView.findViewById(R.id.tvStatusText);
            tvLastActive      = itemView.findViewById(R.id.tvLastActive);
            tvAvailabilityDot = itemView.findViewById(R.id.tvAvailabilityDot);
        }

        // Ide jönnek az adatok és itt írjuk ki őket a sorba
        void bind(MembershipEntity m, String myUserId) {

            // Név: ha van userName, azt írjuk ki, ha nincs, akkor userId-t
            String name = (m.userName != null && !m.userName.isEmpty())
                    ? m.userName
                    : (m.userId != null ? m.userId : "—");

            // Ha ez én vagyok, hozzáírjuk, hogy (You)
            if (myUserId != null && myUserId.equals(m.userId)) {
                name = name + " (You)";
            }

            tvName.setText(name);

            // Státusz szöveg (ha nincs, akkor —)
            String status = (m.statusText != null && !m.statusText.isEmpty()) ? m.statusText : "—";
            tvStatusText.setText(status);

            // Last active idő "emberi" formára alakítása (pl. 5 min ago)
            String relative = toRelativeTime(m.lastActiveAt);
            tvLastActive.setText(relative != null ? ("Last active: " + relative) : "Last active: —");

            // Availability alapján szín (pötty)
            String availability = m.availability != null ? m.availability.toUpperCase() : "";
            int color;
            switch (availability) {
                case "AVAILABLE": color = Color.parseColor("#4CAF50"); break; // zöld
                case "BUSY":      color = Color.parseColor("#F44336"); break; // piros
                case "AWAY":      color = Color.parseColor("#FFC107"); break; // sárga
                default:          color = Color.parseColor("#9E9E9E");  break; // szürke
            }

            // pötty karakter és szín
            tvAvailabilityDot.setText("●");
            tvAvailabilityDot.setTextColor(color);
        }

        // ISO dátumot átalakítunk "relative time"-ra
        // pl: just now, 10 min ago, yesterday
        private String toRelativeTime(String iso) {
            if (iso == null || iso.isEmpty()) return null;

            try {
                Instant then;

                // ha időzónás formátum jön (Z / +02:00 stb.)
                if (iso.endsWith("Z") || iso.contains("+") || (iso.contains("-") && iso.length() > 10)) {
                    then = OffsetDateTime.parse(iso).toInstant();
                } else {
                    // sima ISO instant
                    then = Instant.parse(iso);
                }

                Instant now = Instant.now();
                Duration d = Duration.between(then, now);

                long secs = Math.max(0, d.getSeconds());
                if (secs < 60) return "just now";

                long mins = secs / 60;
                if (mins < 60) return mins + " min ago";

                long hours = mins / 60;
                if (hours < 24) return hours + " h ago";

                long days = hours / 24;
                if (days == 1) return "yesterday";

                return days + " days ago";

            } catch (DateTimeParseException e) {
                // ha nem tudjuk parse-olni az időt, akkor nem írunk semmit
                return null;
            }
        }
    }
}
