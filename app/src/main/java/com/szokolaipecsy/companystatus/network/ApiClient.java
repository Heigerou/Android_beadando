package com.szokolaipecsy.companystatus.network;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static final String BASE_URL = "https://68f7e806f7fb897c66176aaf.mockapi.io/";
    private static Retrofit RETROFIT;
    private static RoomsApi ROOMS_API;
    private static UsersApi USERS_API;
    public static RoomsApi getRoomsApi() {
        return getRetrofit().create(RoomsApi.class);
    }
    private static Retrofit getRetrofit() {
        if (RETROFIT == null) {
            HttpLoggingInterceptor log = new HttpLoggingInterceptor();
            log.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(log)
                    .build();

            RETROFIT = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return RETROFIT;
    }
    public static MembershipsApi getMembershipsApi() {
        return getRetrofit().create(MembershipsApi.class);
    }
    public static UsersApi users() {
        if (USERS_API == null) USERS_API = getRetrofit().create(UsersApi.class);
        return USERS_API;
    }
    public static RoomsApi rooms() {
        if (ROOMS_API == null) {
            HttpLoggingInterceptor log = new HttpLoggingInterceptor();
            log.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(log)
                    .build();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            ROOMS_API = retrofit.create(RoomsApi.class);
        }
        return ROOMS_API;
    }
}
