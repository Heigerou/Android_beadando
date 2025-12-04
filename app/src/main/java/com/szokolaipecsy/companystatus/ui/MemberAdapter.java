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
import com.szokolaipecsy.companystatus.data.UserProfile;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.MemberVH> {

    public interface OnMemberClick {
        void onClick(String userId);
    }
    private final List<MembershipEntity> data = new ArrayList<>();
    private final OnMemberClick click;

    private String myUserId = null;
    private String myUserName = null;

    public void setMyProfile(String userId, String name) {
        this.myUserId = userId;
        this.myUserName = (name != null && !name.isEmpty()) ? name : null;
        notifyDataSetChanged();
    }

    public MemberAdapter(@NonNull OnMemberClick click) {
        this.click = click;
    }

    public void submit(List<MembershipEntity> items) {
        data.clear();
        if (items != null) data.addAll(items);

        if (myUserId != null && myUserName != null) {
            for (MembershipEntity m : data) {
                if (m.userId.equals(myUserId)) {
                    m.userName = myUserName;
                }
            }
        }

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

            if (mine != null && index > 0) {
                data.remove(index);
                data.add(0, mine);
            }
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MemberVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_member, parent, false);
        return new MemberVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberVH holder, int position) {
        MembershipEntity m = data.get(position);
        holder.bind(m, myUserId);
        holder.itemView.setOnClickListener(v -> {
            if (click != null) click.onClick(m.userId);
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class MemberVH extends RecyclerView.ViewHolder {
        final TextView tvName;
        final TextView tvStatusText;
        final TextView tvLastActive;
        final TextView tvAvailabilityDot;

        MemberVH(@NonNull View itemView) {
            super(itemView);
            tvName          = itemView.findViewById(R.id.tvName);
            tvStatusText    = itemView.findViewById(R.id.tvStatusText);
            tvLastActive    = itemView.findViewById(R.id.tvLastActive);
            tvAvailabilityDot = itemView.findViewById(R.id.tvAvailabilityDot);
        }

        void bind(MembershipEntity m, String myUserId) {

            String name = (m.userName != null && !m.userName.isEmpty())
                    ? m.userName
                    : (m.userId != null ? m.userId : "—");

            if (myUserId != null && myUserId.equals(m.userId)) {
                name = name + " (You)";
            }

            tvName.setText(name);

            String status = (m.statusText != null && !m.statusText.isEmpty()) ? m.statusText : "—";
            tvStatusText.setText(status);

            String relative = toRelativeTime(m.lastActiveAt);
            tvLastActive.setText(relative != null ? ("Last active: " + relative) : "Last active: —");

            String availability = m.availability != null ? m.availability.toUpperCase() : "";
            int color;
            switch (availability) {
                case "AVAILABLE": color = Color.parseColor("#4CAF50"); break;
                case "BUSY":      color = Color.parseColor("#F44336"); break;
                case "AWAY":      color = Color.parseColor("#FFC107"); break;
                default:          color = Color.parseColor("#9E9E9E");  break;
            }
            tvAvailabilityDot.setText("●");
            tvAvailabilityDot.setTextColor(color);
        }

        private String toRelativeTime(String iso) {
            if (iso == null || iso.isEmpty()) return null;
            try {
                Instant then;
                if (iso.endsWith("Z") || iso.contains("+") || (iso.contains("-") && iso.length() > 10)) {
                    then = OffsetDateTime.parse(iso).toInstant();
                } else {
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
                return null;
            }
        }
    }
}
