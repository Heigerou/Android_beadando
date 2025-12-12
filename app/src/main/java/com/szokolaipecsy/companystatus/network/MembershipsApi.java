package com.szokolaipecsy.companystatus.network;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.Path;
import retrofit2.http.POST;
import retrofit2.http.Query;

// Ez az API interface a memberships végpontokat írja le
// Itt mondjuk meg a Retrofitnek, milyen HTTP kéréseket lehet indítani
public interface MembershipsApi {

    // Egy szoba összes tagjának lekérése
    // GET /memberships?roomId=...
    @GET("memberships")
    Call<List<MembershipDto>> getByRoom(@Query("roomId") String roomId);

    // Új membership létrehozása (szobába belépés)
    // POST /memberships
    // A body-ban küldjük a MembershipDto-t
    @POST("memberships")
    Call<MembershipDto> create(@Body MembershipDto body);

    // Membership frissítése
    // PATCH /memberships/{id}
    // Csak a módosított mezőket küldjük (pl. státusz, lastActive)
    @PATCH("memberships/{id}")
    Call<MembershipDto> update(@Path("id") String id, @Body Map<String, Object> patch);

    // Kilépés egy szobából
    // DELETE /memberships/{id}
    @DELETE("memberships/{id}")
    Call<Void> leave(@Path("id") String id);

    // Egy felhasználó összes membershipjének lekérése
    // GET /memberships?userId=...
    @GET("memberships")
    Call<List<MembershipDto>> getByUser(@Query("userId") String userId);

}
