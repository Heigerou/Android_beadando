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


public class RoomDetailViewModel extends AndroidViewModel {

    private final RoomsApi roomsApi = ApiClient.getRoomsApi();
    private final MembershipsRepository membershipsRepo;
    private final AuthManager auth;
    private final UserProfileDao profileDao;
    public final LiveData<UserProfile> myProfile;
    public final String myUserId;

    private String currentRoomId;

    public final MutableLiveData<RoomDto> room = new MutableLiveData<>();
    public LiveData<List<MembershipEntity>> members;

    public final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    public final MutableLiveData<String> error = new MutableLiveData<>(null);

    private final MediatorLiveData<Boolean> _isJoined = new MediatorLiveData<>();
    private final MediatorLiveData<String> _myMembershipId = new MediatorLiveData<>();
    public final LiveData<Boolean> isJoined = _isJoined;
    public final LiveData<String> myMembershipId = _myMembershipId;
    public void refreshRoom() {
        if (currentRoomId == null) return;
        loadRoom(currentRoomId);
    }
    public RoomDetailViewModel(@NonNull Application app) {
        super(app);
        membershipsRepo = new MembershipsRepository(app.getApplicationContext());
        auth = AuthManager.getInstance(app.getApplicationContext());

        profileDao = DbProvider.get(app.getApplicationContext()).userProfileDao();
        myUserId = auth.getUserId();
        myProfile = profileDao.observeByUserId(myUserId);

        _isJoined.setValue(false);
        _myMembershipId.setValue(null);
    }


    public void start(String roomId) {
        currentRoomId = roomId;

        loadRoom(roomId);

        members = membershipsRepo.observeMembers(roomId);

        _isJoined.addSource(members, list -> {
            String myId = auth.getUserId();
            String myName = auth.getUserName();
            String foundMembershipId = null;
            boolean joined = false;

            if (list != null) {
                for (MembershipEntity m : list) {
                    boolean sameUserId = myId != null && myId.equals(m.userId);
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

            _isJoined.setValue(joined);
            _myMembershipId.setValue(foundMembershipId);
            loading.setValue(false);
        });

        refreshMembers();
    }

    public void refreshMembers() {
        if (currentRoomId == null) return;
        membershipsRepo.syncMembers(currentRoomId);
    }

    public void joinRoom() {
        if (currentRoomId == null) return;
        loading.setValue(true);

        membershipsRepo.joinRoom(
                currentRoomId,
                () -> {
                    refreshMembers();
                },
                msg -> {
                    loading.postValue(false);
                    error.postValue(msg != null ? msg : "Join failed");
                }
        );
    }

    public void updateLastActiveIfJoined() {
        String id = _myMembershipId.getValue();
        if (id != null) membershipsRepo.updateLastActive(id);
    }
    public void updateMyStatus(String availability, String statusText) {
        String id = _myMembershipId.getValue();
        if (id == null) {
            error.setValue("You are not a member of this room");
            return;
        }

        loading.setValue(true);

        membershipsRepo.updateStatus(
                id,
                availability,
                statusText,
                () -> {
                    refreshMembers();
                },
                msg -> {
                    loading.postValue(false);
                    error.postValue(msg != null ? msg : "Failed to update status");
                }
        );
    }

    private void loadRoom(String roomId) {
        loading.setValue(true);
        roomsApi.getRoomById(roomId).enqueue(new Callback<RoomDto>() {
            @Override public void onResponse(Call<RoomDto> call, Response<RoomDto> resp) {
                if (resp.isSuccessful() && resp.body() != null) {
                    room.postValue(resp.body());
                } else {
                    error.postValue("Failed to load room");
                }
                loading.postValue(false);
            }
            @Override public void onFailure(Call<RoomDto> call, Throwable t) {
                error.postValue(t.getMessage());
                loading.postValue(false);
            }
        });
    }
}
