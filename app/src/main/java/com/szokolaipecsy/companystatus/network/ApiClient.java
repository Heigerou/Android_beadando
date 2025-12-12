package com.szokolaipecsy.companystatus.network;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

// Ez az ApiClient felel a hálózati kommunikációért
// Itt állítjuk be a Retrofitet és az API végpontokat
public class ApiClient {

    // A backend (MockAPI) alap URL-je
    private static final String BASE_URL = "https://68f7e806f7fb897c66176aaf.mockapi.io/";

    // Retrofit példány (singleton jellegű)
    private static Retrofit RETROFIT;

    // API interfészek példányai
    private static RoomsApi ROOMS_API;
    private static UsersApi USERS_API;

    // Rooms API lekérése
    // A Retrofit hozza létre az implementációt
    public static RoomsApi getRoomsApi() {
        return getRetrofit().create(RoomsApi.class);
    }

    // Retrofit példány létrehozása (ha még nincs)
    private static Retrofit getRetrofit() {
        if (RETROFIT == null) {

            // Log interceptor
            // Kiírja a requesteket és response-okat (fejlesztéshez nagyon hasznos)
            HttpLoggingInterceptor log = new HttpLoggingInterceptor();
            log.setLevel(HttpLoggingInterceptor.Level.BODY);

            // OkHttp kliens, interceptorral
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(log)
                    .build();

            // Retrofit felépítése
            RETROFIT = new Retrofit.Builder()
                    .baseUrl(BASE_URL) // alap URL
                    .client(client)    // HTTP kliens
                    .addConverterFactory(GsonConverterFactory.create()) // JSON <-> objektum
                    .build();
        }
        return RETROFIT;
    }

    // Memberships API lekérése
    public static MembershipsApi getMembershipsApi() {
        return getRetrofit().create(MembershipsApi.class);
    }

    // Users API lekérése
    // Itt eltároljuk a példányt, hogy ne hozzuk létre mindig újra
    public static UsersApi users() {
        if (USERS_API == null) {
            USERS_API = getRetrofit().create(UsersApi.class);
        }
        return USERS_API;
    }

    // Rooms API lekérése (külön példány)
    // Itt külön Retrofit példányt hozunk létre
    public static RoomsApi rooms() {
        if (ROOMS_API == null) {

            // Log interceptor a szobák API-hoz
            HttpLoggingInterceptor log = new HttpLoggingInterceptor();
            log.setLevel(HttpLoggingInterceptor.Level.BODY);

            // OkHttp kliens
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(log)
                    .build();

            // Retrofit létrehozása
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            // RoomsApi implementáció elkészítése
            ROOMS_API = retrofit.create(RoomsApi.class);
        }
        return ROOMS_API;
    }
}
