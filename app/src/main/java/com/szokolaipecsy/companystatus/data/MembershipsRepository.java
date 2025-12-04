package com.szokolaipecsy.companystatus.data;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.szokolaipecsy.companystatus.auth.AuthManager;
import com.szokolaipecsy.companystatus.network.ApiClient;
import com.szokolaipecsy.companystatus.network.MembershipDto;
import com.szokolaipecsy.companystatus.network.MembershipsApi;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MembershipsRepository {

    private final MembershipsApi api;
    private final MembershipDao dao;
    private final AuthManager auth;
    private final Context appContext;

    public MembershipsRepository(Context context) {
        appContext = context.getApplicationContext();
        api = ApiClient.getMembershipsApi();
        dao = DbProvider.get(appContext).membershipDao();
        auth = AuthManager.getInstance(appContext);
    }

    public LiveData<List<MembershipEntity>> observeMembers(String roomId) {
        return dao.observeByRoom(roomId);
    }

    public void syncMembers(String roomId) {
        api.getByRoom(roomId).enqueue(new Callback<List<MembershipDto>>() {
            @Override
            public void onResponse(Call<List<MembershipDto>> call, Response<List<MembershipDto>> response) {
                if (!response.isSuccessful() || response.body() == null) return;

                String myId = auth.getUserId();
                UserProfileDao userDao = DbProvider.get(appContext).userProfileDao();
                UserProfile myProfile = null;
                if (myId != null) {
                    try {
                        myProfile = userDao.getSync(myId);
                    } catch (Exception ignored) { }
                }

                List<MembershipEntity> list = new ArrayList<>();
                for (MembershipDto d : response.body()) {
                    MembershipEntity e = new MembershipEntity();
                    e.id = d.id;
                    e.roomId = d.roomId;
                    e.userId = d.userId;
                    e.role = d.role;
                    e.joinedAt = d.joinedAt;
                    e.lastActiveAt = d.lastActiveAt;

                    if (myId != null && myId.equals(d.userId)
                            && myProfile != null
                            && myProfile.name != null
                            && !myProfile.name.isEmpty()) {
                        e.userName = myProfile.name;
                    } else {
                        e.userName = d.userName;
                    }

                    e.statusText = d.statusText;
                    e.availability = d.availability;
                    list.add(e);
                }

                AppDatabase.EXECUTOR.execute(() -> dao.upsertAll(list));
            }

            @Override
            public void onFailure(Call<List<MembershipDto>> call, Throwable t) { }
        });
    }

    public void joinRoom(String roomId, Runnable onOk, java.util.function.Consumer<String> onError) {

        String myUserId = auth.getUserId();

        AppDatabase.EXECUTOR.execute(() -> {
            UserProfileDao userDao = DbProvider.get(appContext).userProfileDao();
            UserProfile profile = userDao.getSync(myUserId);

            String myName = (profile != null && profile.name != null && !profile.name.isEmpty())
                    ? profile.name
                    : myUserId;

            android.content.SharedPreferences prefs =
                    appContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
            int idx = prefs.getInt("pref_default_status", 0);

            String availability;
            String statusText;
            switch (idx) {
                case 1:
                    availability = "BUSY";
                    statusText = "Busy";
                    break;
                case 2:
                    availability = "AWAY";
                    statusText = "Away";
                    break;
                default:
                    availability = "AVAILABLE";
                    statusText = "Online";
                    break;
            }

            MembershipDto body = new MembershipDto();
            body.roomId = roomId;
            body.userId = myUserId;
            body.userName = myName;
            body.role = "member";
            body.joinedAt = java.time.Instant.now().toString();
            body.lastActiveAt = body.joinedAt;
            body.availability = availability;
            body.statusText = statusText;

            api.create(body).enqueue(new Callback<MembershipDto>() {
                @Override
                public void onResponse(Call<MembershipDto> call, Response<MembershipDto> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        MembershipDto d = response.body();
                        MembershipEntity e = new MembershipEntity();

                        e.id = d.id;
                        e.roomId = d.roomId;
                        e.userId = d.userId;
                        e.userName = d.userName;
                        e.role = d.role;
                        e.joinedAt = d.joinedAt;
                        e.lastActiveAt = d.lastActiveAt;
                        e.statusText = d.statusText;
                        e.availability = d.availability;

                        AppDatabase.EXECUTOR.execute(() -> dao.upsert(e));
                        if (onOk != null) onOk.run();
                    } else if (onError != null) {
                        onError.accept("Join failed");
                    }
                }

                @Override
                public void onFailure(Call<MembershipDto> call, Throwable t) {
                    if (onError != null) onError.accept(t.getMessage());
                }
            });
        });
    }

    public void updateLastActive(String membershipId) {
        Map<String, Object> patch = new HashMap<>();
        patch.put("lastActiveAt", Instant.now().toString());
        api.update(membershipId, patch).enqueue(new Callback<MembershipDto>() {
            @Override public void onResponse(Call<MembershipDto> call, Response<MembershipDto> response) { }
            @Override public void onFailure(Call<MembershipDto> call, Throwable t) { }
        });
    }

    public void updateStatus(
            String membershipId,
            String availability,
            String statusText,
            Runnable onOk,
            java.util.function.Consumer<String> onError
    ) {
        if (membershipId == null) {
            if (onError != null) onError.accept("No membership id");
            return;
        }

        Map<String, Object> patch = new HashMap<>();
        if (availability != null && !availability.isEmpty()) {
            patch.put("availability", availability);
        }
        if (statusText != null) {
            patch.put("statusText", statusText);
        }
        patch.put("lastActiveAt", Instant.now().toString());

        api.update(membershipId, patch).enqueue(new Callback<MembershipDto>() {
            @Override
            public void onResponse(Call<MembershipDto> call, Response<MembershipDto> response) {
                if (response.isSuccessful()) {
                    if (onOk != null) onOk.run();
                } else if (onError != null) {
                    onError.accept("Failed to update status");
                }
            }

            @Override
            public void onFailure(Call<MembershipDto> call, Throwable t) {
                if (onError != null) onError.accept(t.getMessage());
            }
        });
    }
    public void updateUserNameEverywhere(String userId, String newDisplayName) {
        if (userId == null || newDisplayName == null || newDisplayName.isEmpty()) {
            return;
        }

        api.getByUser(userId).enqueue(new Callback<List<MembershipDto>>() {
            @Override
            public void onResponse(Call<List<MembershipDto>> call,
                                   Response<List<MembershipDto>> response) {
                if (!response.isSuccessful() || response.body() == null) return;

                List<MembershipDto> list = response.body();

                for (MembershipDto m : list) {
                    if (m == null || m.id == null) continue;

                    Map<String, Object> patch = new HashMap<>();
                    patch.put("userName", newDisplayName);

                    api.update(m.id, patch).enqueue(new Callback<MembershipDto>() {
                        @Override public void onResponse(Call<MembershipDto> call,
                                                         Response<MembershipDto> response) {
                        }
                        @Override public void onFailure(Call<MembershipDto> call, Throwable t) { }
                    });
                }

                AppDatabase.EXECUTOR.execute(() -> {
                    dao.updateNameForUser(userId, newDisplayName);
                });
            }

            @Override
            public void onFailure(Call<List<MembershipDto>> call, Throwable t) {
            }
        });
    }
}
