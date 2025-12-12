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

// Ez a repository a "memberships" dolgokat intézi
// Vagyis: szobába belépés, taglista lekérés, státusz frissítés
public class MembershipsRepository {

    // api = netes hívások (MockAPI / backend)
    private final MembershipsApi api;

    // dao = helyi adatbázis (Room) műveletek
    private final MembershipDao dao;

    // auth = ki vagyok én? (bejelentkezett user id stb.)
    private final AuthManager auth;

    // appContext = biztosan nem Activity context (ne legyen memory leak)
    private final Context appContext;

    public MembershipsRepository(Context context) {
        // mindig application contextet használunk
        appContext = context.getApplicationContext();

        // api példány a netes kérésekhez
        api = ApiClient.getMembershipsApi();

        // dao a helyi adatbázishoz
        dao = DbProvider.get(appContext).membershipDao();

        // auth manager (ki van belépve)
        auth = AuthManager.getInstance(appContext);
    }

    // UI innen figyeli a taglistát
    // LiveData -> ha változik a DB-ben, a lista automatikusan frissül
    public LiveData<List<MembershipEntity>> observeMembers(String roomId) {
        return dao.observeByRoom(roomId);
    }

    // Taglista frissítés a szerverről (API -> DB)
    public void syncMembers(String roomId) {
        // getByRoom = lekéri a szoba tagjait netről
        api.getByRoom(roomId).enqueue(new Callback<List<MembershipDto>>() {
            @Override
            public void onResponse(Call<List<MembershipDto>> call, Response<List<MembershipDto>> response) {
                // ha rossz válasz vagy nincs body -> kilépünk
                if (!response.isSuccessful() || response.body() == null) return;

                // lekérjük a saját userId-t (mert ha "én" vagyok a listában, máshogy kezeljük a nevem)
                String myId = auth.getUserId();

                // userDao: a saját profilomat a helyi DB-ből tudom lekérni
                UserProfileDao userDao = DbProvider.get(appContext).userProfileDao();

                // myProfile = a saját profilom (ha van)
                UserProfile myProfile = null;
                if (myId != null) {
                    try {
                        // getSync = gyorsan lekéri szinkron módon (DB-ből)
                        myProfile = userDao.getSync(myId);
                    } catch (Exception ignored) { }
                }

                // itt alakítjuk át a netes DTO-kat DB Entitykké
                List<MembershipEntity> list = new ArrayList<>();
                for (MembershipDto d : response.body()) {
                    MembershipEntity e = new MembershipEntity();

                    // alap mezők átmásolása
                    e.id = d.id;
                    e.roomId = d.roomId;
                    e.userId = d.userId;
                    e.role = d.role;
                    e.joinedAt = d.joinedAt;
                    e.lastActiveAt = d.lastActiveAt;

                    // ha ez a saját membershipem:
                    // akkor inkább a saját profil név legyen, ne a szerverről jövő (biztosabb)
                    if (myId != null && myId.equals(d.userId)
                            && myProfile != null
                            && myProfile.name != null
                            && !myProfile.name.isEmpty()) {
                        e.userName = myProfile.name;
                    } else {
                        // ha nem én vagyok, akkor marad a szerverről jövő név
                        e.userName = d.userName;
                    }

                    // státusz mezők
                    e.statusText = d.statusText;
                    e.availability = d.availability;

                    list.add(e);
                }

                // DB mentés háttérszálon (ne UI-n)
                AppDatabase.EXECUTOR.execute(() -> dao.upsertAll(list));
            }

            @Override
            public void onFailure(Call<List<MembershipDto>> call, Throwable t) {
                // itt most direkt nem csinálunk semmit (csak csendben fail)
            }
        });
    }

    // Szobába csatlakozás
    // onOk = ha sikerül (pl. UI frissítés)
    // onError = ha hiba van (pl. kiírjuk toast/snackbar)
    public void joinRoom(String roomId, Runnable onOk, java.util.function.Consumer<String> onError) {

        // saját userId
        String myUserId = auth.getUserId();

        // DB műveletek háttérszálon
        AppDatabase.EXECUTOR.execute(() -> {

            // saját profil lekérés (hogy legyen normális név)
            UserProfileDao userDao = DbProvider.get(appContext).userProfileDao();
            UserProfile profile = userDao.getSync(myUserId);

            // ha van név, azt használjuk, különben legalább a userId-t
            String myName = (profile != null && profile.name != null && !profile.name.isEmpty())
                    ? profile.name
                    : myUserId;

            // app_prefs beállítás: alap státusz (pl. Online/Busy/Away)
            android.content.SharedPreferences prefs =
                    appContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
            int idx = prefs.getInt("pref_default_status", 0);

            // ebből kiszámoljuk az availability + statusText értéket
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

            // összeállítjuk a küldendő adatot (DTO)
            MembershipDto body = new MembershipDto();
            body.roomId = roomId;
            body.userId = myUserId;
            body.userName = myName;
            body.role = "member";
            body.joinedAt = java.time.Instant.now().toString();
            body.lastActiveAt = body.joinedAt;
            body.availability = availability;
            body.statusText = statusText;

            // create = felvesszük a membershipet a szerveren
            api.create(body).enqueue(new Callback<MembershipDto>() {
                @Override
                public void onResponse(Call<MembershipDto> call, Response<MembershipDto> response) {
                    if (response.isSuccessful() && response.body() != null) {

                        // ha siker, akkor mentjük a DB-be is
                        MembershipDto d = response.body();
                        MembershipEntity e = new MembershipEntity();

                        // átmásoljuk a mezőket
                        e.id = d.id;
                        e.roomId = d.roomId;
                        e.userId = d.userId;
                        e.userName = d.userName;
                        e.role = d.role;
                        e.joinedAt = d.joinedAt;
                        e.lastActiveAt = d.lastActiveAt;
                        e.statusText = d.statusText;
                        e.availability = d.availability;

                        // DB mentés háttérszálon
                        AppDatabase.EXECUTOR.execute(() -> dao.upsert(e));

                        // UI callback: siker
                        if (onOk != null) onOk.run();

                    } else if (onError != null) {
                        // UI callback: hiba szöveg
                        onError.accept("Join failed");
                    }
                }

                @Override
                public void onFailure(Call<MembershipDto> call, Throwable t) {
                    // UI callback: hálózati hiba
                    if (onError != null) onError.accept(t.getMessage());
                }
            });
        });
    }

    // Csak a "lastActiveAt" frissítése (pl. ha megnyitom a szobát)
    public void updateLastActive(String membershipId) {
        // patch = csak pár mezőt küldünk fel, nem az egészet
        Map<String, Object> patch = new HashMap<>();
        patch.put("lastActiveAt", Instant.now().toString());

        // update (PATCH) hívás
        api.update(membershipId, patch).enqueue(new Callback<MembershipDto>() {
            @Override public void onResponse(Call<MembershipDto> call, Response<MembershipDto> response) { }
            @Override public void onFailure(Call<MembershipDto> call, Throwable t) { }
        });
    }

    // Státusz frissítés (availability + statusText + lastActiveAt)
    public void updateStatus(
            String membershipId,
            String availability,
            String statusText,
            Runnable onOk,
            java.util.function.Consumer<String> onError
    ) {
        // ha nincs membershipId, nem tudunk mit frissíteni
        if (membershipId == null) {
            if (onError != null) onError.accept("No membership id");
            return;
        }

        // patch map: csak azt küldjük, ami tényleg változik
        Map<String, Object> patch = new HashMap<>();

        // availability csak akkor megy fel, ha van értelme
        if (availability != null && !availability.isEmpty()) {
            patch.put("availability", availability);
        }

        // statusText akkor is mehet, ha üresre akarjuk állítani
        if (statusText != null) {
            patch.put("statusText", statusText);
        }

        // mindig frissítjük az aktivitást is
        patch.put("lastActiveAt", Instant.now().toString());

        // update (PATCH) hívás
        api.update(membershipId, patch).enqueue(new Callback<MembershipDto>() {
            @Override
            public void onResponse(Call<MembershipDto> call, Response<MembershipDto> response) {
                if (response.isSuccessful()) {
                    // siker -> szólunk a UI-nak
                    if (onOk != null) onOk.run();
                } else if (onError != null) {
                    // hiba -> szólunk a UI-nak
                    onError.accept("Failed to update status");
                }
            }

            @Override
            public void onFailure(Call<MembershipDto> call, Throwable t) {
                // hálózati hiba -> szólunk a UI-nak
                if (onError != null) onError.accept(t.getMessage());
            }
        });
    }

    // Ha a user megváltoztatja a nevét, akkor minden szobában átírjuk
    // 1) szerveren (API)
    // 2) helyi DB-ben is
    public void updateUserNameEverywhere(String userId, String newDisplayName) {
        // ha nincs userId vagy üres név, akkor nincs mit csinálni
        if (userId == null || newDisplayName == null || newDisplayName.isEmpty()) {
            return;
        }

        // lekérjük az összes membershipet userId alapján
        api.getByUser(userId).enqueue(new Callback<List<MembershipDto>>() {
            @Override
            public void onResponse(Call<List<MembershipDto>> call,
                                   Response<List<MembershipDto>> response) {
                if (!response.isSuccessful() || response.body() == null) return;

                List<MembershipDto> list = response.body();

                // végigmegyünk rajtuk, és mindegyiket frissítjük
                for (MembershipDto m : list) {
                    if (m == null || m.id == null) continue;

                    Map<String, Object> patch = new HashMap<>();
                    patch.put("userName", newDisplayName);

                    // update a szerveren
                    api.update(m.id, patch).enqueue(new Callback<MembershipDto>() {
                        @Override public void onResponse(Call<MembershipDto> call,
                                                         Response<MembershipDto> response) {
                        }
                        @Override public void onFailure(Call<MembershipDto> call, Throwable t) { }
                    });
                }

                // helyi DB-ben is átírjuk a nevet mindenhol
                AppDatabase.EXECUTOR.execute(() -> {
                    dao.updateNameForUser(userId, newDisplayName);
                });
            }

            @Override
            public void onFailure(Call<List<MembershipDto>> call, Throwable t) {
                // most itt is csendben fail
            }
        });
    }
}
