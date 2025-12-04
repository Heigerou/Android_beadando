package com.szokolaipecsy.companystatus.network;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface RoomsApi {

    @GET("rooms/{id}")
    Call<RoomDto> getRoomById(@Path("id") String id);

    @GET("rooms")
    Call<List<RoomDto>> getRooms();

    @POST("rooms")
    Call<RoomDto> createRoom(@Body RoomCreateRequest body);
}
