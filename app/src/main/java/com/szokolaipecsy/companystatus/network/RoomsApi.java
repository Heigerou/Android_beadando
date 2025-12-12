package com.szokolaipecsy.companystatus.network;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

// Ez az API interface a szobákhoz tartozó végpontokat írja le
// A Retrofit ebből csinál működő hálózati hívásokat
public interface RoomsApi {

    // Egy konkrét szoba lekérése ID alapján
    // GET /rooms/{id}
    @GET("rooms/{id}")
    Call<RoomDto> getRoomById(@Path("id") String id);

    // Összes szoba lekérése
    // GET /rooms
    @GET("rooms")
    Call<List<RoomDto>> getRooms();

    // Új szoba létrehozása
    // POST /rooms
    // A body-ban küldjük a RoomCreateRequest-et
    @POST("rooms")
    Call<RoomDto> createRoom(@Body RoomCreateRequest body);
}
