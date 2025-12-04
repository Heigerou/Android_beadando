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

public interface MembershipsApi {

    @GET("memberships")
    Call<List<MembershipDto>> getByRoom(@Query("roomId") String roomId);

    @POST("memberships")
    Call<MembershipDto> create(@Body MembershipDto body);

    @PATCH("memberships/{id}")
    Call<MembershipDto> update(@Path("id") String id, @Body Map<String, Object> patch);

    @DELETE("memberships/{id}")
    Call<Void> leave(@Path("id") String id);
    @GET("memberships")
    Call<List<MembershipDto>> getByUser(@Query("userId") String userId);

}
