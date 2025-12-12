package com.szokolaipecsy.companystatus.network;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

// Ez az API interface a felhasználókhoz tartozó műveleteket kezeli
// Bejelentkezéshez és regisztrációhoz használjuk
public interface UsersApi {

    // Felhasználó keresése email + jelszó alapján
    // GET /api/users?email=...&password=...
    // Bejelentkezéskor ezt hívjuk
    @GET("api/users")
    Call<List<UserDto>> findUser(@Query("email") String email,
                                 @Query("password") String password);

    // Új felhasználó létrehozása
    // POST /api/users
    // Regisztrációnál ezt hívjuk
    @POST("api/users")
    Call<UserDto> createUser(@Body UserDto body);
}
