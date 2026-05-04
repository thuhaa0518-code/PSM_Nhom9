package com.example.pms_nhom9.api;

import android.content.Context;
import android.content.SharedPreferences;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    // Đổi IP này thành IP máy tính chạy server khi test trên điện thoại thật
    // Nếu dùng emulator: 10.0.2.2 (trỏ về localhost của máy host)
    public static final String BASE_URL = "http://192.168.1.13:3000/";

    private static Retrofit retrofit;

    public static Retrofit getInstance(Context ctx) {
        if (retrofit == null) {
            SharedPreferences prefs = ctx.getSharedPreferences("psm_prefs", Context.MODE_PRIVATE);
            String token = prefs.getString("auth_token", "");

            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(chain -> {
                        Request original = chain.request();
                        Request request = original.newBuilder()
                                .header("Authorization", "Bearer " + token)
                                .header("Content-Type", "application/json")
                                .build();
                        return chain.proceed(request);
                    })
                    .addInterceptor(logging)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    // Reset khi logout hoặc token thay đổi
    public static void reset() {
        retrofit = null;
    }

    public static ApiService getService(Context ctx) {
        return getInstance(ctx).create(ApiService.class);
    }
}
