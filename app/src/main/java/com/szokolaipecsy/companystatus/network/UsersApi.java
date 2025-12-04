package com.szokolaipecsy.companystatus.network;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface UsersApi {
    @GET("api/users")
    Call<List<UserDto>> findUser(@Query("email") String email,
                                 @Query("password") String password);

    @POST("api/users")
    Call<UserDto> createUser(@Body UserDto body);
}
