package com.szokolaipecsy.companystatus.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.szokolaipecsy.companystatus.auth.AuthManager;
import com.szokolaipecsy.companystatus.data.MembershipEntity;
import com.szokolaipecsy.companystatus.data.MembershipsRepository;
import com.szokolaipecsy.companystatus.network.ApiClient;
import com.szokolaipecsy.companystatus.network.RoomDto;
import com.szokolaipecsy.companystatus.network.RoomsApi;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.szokolaipecsy.companystatus.data.DbProvider;
import com.szokolaipecsy.companystatus.data.UserProfile;
import com.szokolaipecsy.companystatus.data.UserProfileDao;

// Ez a RoomDetailViewModel
// A RoomDetailFragmentnek ad adatot és itt van a "logika":
// - szoba lekérése API-ról
// - tagok (memberships) figyelése Room DB-ből
// - belépés (join)
// - lastActive frissítés
// - státusz frissítés
public class RoomDetailViewModel extends AndroidViewModel {

    // Rooms API (szoba részletek lekérése)
    private final RoomsApi roomsApi = ApiClient.getRoomsApi();

    // Membership repo: tagságok lekérése + szinkron MockAPI <-> Room DB
    private final MembershipsRepository membershipsRepo;

    // Auth (ki van belépve)
    private final AuthManager auth;

    // Profil DAO (helyi profil adatok)
    private final UserProfileDao profileDao;

    // Saját profil LiveData (hogy pl. MemberAdapter megkapja a nevemet)
    public final LiveData<UserProfile> myProfile;

    // Saját userId (Authból)
    public final String myUserId;

    // Melyik szobában vagyunk most (current)
    private String currentRoomId;

    // Szoba adatok LiveData (RoomDto)
    public final MutableLiveData<RoomDto> room = new MutableLiveData<>();

    // Tagok listája (Room DB-ből jön LiveData-val)
    public LiveData<List<MembershipEntity>> members;

    // Betöltés állapot + hiba szöveg
    public final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    public final MutableLiveData<String> error = new MutableLiveData<>(null);

    // MediatorLiveData: több forrásból tud "összerakni" információt
    // Itt a tagok listájából számoljuk ki:
    // - beléptem-e a szobába
    // - mi az én membership ID-m
    private final MediatorLiveData<Boolean> _isJoined = new MediatorLiveData<>();
    private final MediatorLiveData<String> _myMembershipId = new MediatorLiveData<>();

    // Ezeket figyeli a Fragment
    public final LiveData<Boolean> isJoined = _isJoined;
    public final LiveData<String> myMembershipId = _myMembershipId;

    // Külső hívás: csak újratölti a szobát (ha kell)
    public void refreshRoom() {
        if (currentRoomId == null) return;
        loadRoom(currentRoomId);
    }

    // Konstruktor: itt inicializáljuk a repót, authot, profil figyelést
    public RoomDetailViewModel(@NonNull Application app) {
        super(app);

        membershipsRepo = new MembershipsRepository(app.getApplicationContext());
        auth = AuthManager.getInstance(app.getApplicationContext());

        // Saját profil figyelése Room DB-ből
        profileDao = DbProvider.get(app.getApplicationContext()).userProfileDao();
        myUserId = auth.getUserId();
        myProfile = profileDao.observeByUserId(myUserId);

        // alapértékek
        _isJoined.setValue(false);
        _myMembershipId.setValue(null);
    }

    // Ezt hívja a Fragment, amikor megnyílik a szoba
    public void start(String roomId) {

        // elmentjük a roomId-t, hogy később is tudjunk frissíteni
        currentRoomId = roomId;

        // szoba adatainak első betöltése
        loadRoom(roomId);

        // members LiveData: a Room DB-t figyeljük (memberships táblából)
        members = membershipsRepo.observeMembers(roomId);

        // Mediator: amikor a members lista változik, akkor nézzük meg:
        // - benne vagyok-e én is a listában
        // - ha igen, mi az én membership id-m
        _isJoined.addSource(members, list -> {

            String myId = auth.getUserId();
            String myName = auth.getUserName();

            String foundMembershipId = null;
            boolean joined = false;

            if (list != null) {
                for (MembershipEntity m : list) {

                    // 1) ellenőrzés userId alapján
                    boolean sameUserId = myId != null && myId.equals(m.userId);

                    // 2) extra ellenőrzés userName alapján (ha valamiért ez van meg biztosan)
                    boolean sameUserName = myName != null && !myName.isEmpty()
                            && m.userName != null && !m.userName.isEmpty()
                            && myName.equals(m.userName);

                    if (sameUserId || sameUserName) {
                        joined = true;
                        foundMembershipId = m.id;
                        break;
                    }
                }
            }

            // eredmény kiírás LiveData-ba (Fragment ezt figyeli)
            _isJoined.setValue(joined);
            _myMembershipId.setValue(foundMembershipId);

            // ha a lista megjött, akkor nem töltünk már
            loading.setValue(false);
        });

        // első tagság frissítés API-ról -> DB-be
        refreshMembers();
    }

    // Tagok frissítése (API -> DB)
    public void refreshMembers() {
        if (currentRoomId == null) return;
        membershipsRepo.syncMembers(currentRoomId);
    }

    // Belépés a szobába (membership létrehozás API-n)
    public void joinRoom() {
        if (currentRoomId == null) return;

        loading.setValue(true);

        membershipsRepo.joinRoom(
                currentRoomId,

                // siker esetén: újraszinkronizáljuk a tagokat
                () -> {
                    refreshMembers();
                },

                // hiba esetén: loading off + error üzenet
                msg -> {
                    loading.postValue(false);
                    error.postValue(msg != null ? msg : "Join failed");
                }
        );
    }

    // Ha beléptem, frissítjük a lastActiveAt mezőt (API PATCH)
    public void updateLastActiveIfJoined() {
        String id = _myMembershipId.getValue();
        if (id != null) membershipsRepo.updateLastActive(id);
    }

    // Saját státusz frissítése (availability + statusText)
    public void updateMyStatus(String availability, String statusText) {

        String id = _myMembershipId.getValue();

        // ha nincs membership id, akkor nem vagyok tag
        if (id == null) {
            error.setValue("You are not a member of this room");
            return;
        }

        loading.setValue(true);

        membershipsRepo.updateStatus(
                id,
                availability,
                statusText,

                // ok: tagok frissítése
                () -> {
                    refreshMembers();
                },

                // hiba: loading off + üzenet
                msg -> {
                    loading.postValue(false);
                    error.postValue(msg != null ? msg : "Failed to update status");
                }
        );
    }

    // Szoba adat betöltése RoomsApi-ból (Retrofit)
    private void loadRoom(String roomId) {
        loading.setValue(true);

        roomsApi.getRoomById(roomId).enqueue(new Callback<RoomDto>() {

            @Override
            public void onResponse(Call<RoomDto> call, Response<RoomDto> resp) {

                // ha ok és van body, akkor beírjuk a room LiveData-ba
                if (resp.isSuccessful() && resp.body() != null) {
                    room.postValue(resp.body());
                } else {
                    error.postValue("Failed to load room");
                }

                loading.postValue(false);
            }

            @Override
            public void onFailure(Call<RoomDto> call, Throwable t) {
                // network error / timeout stb.
                error.postValue(t.getMessage());
                loading.postValue(false);
            }
        });
    }
}
