package com.example.pms_nhom9.api;

import com.example.pms_nhom9.api.model.EventRequest;
import com.example.pms_nhom9.api.model.EventResponse;
import com.example.pms_nhom9.api.model.LoginRequest;
import com.example.pms_nhom9.api.model.LoginResponse;
import com.example.pms_nhom9.api.model.RegisterRequest;
import com.example.pms_nhom9.api.model.MessageResponse;
import com.example.pms_nhom9.api.model.UserResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    // ── Auth ──────────────────────────────────────────────────────────
    @POST("api/auth/login")
    Call<LoginResponse> login(@Body LoginRequest body);

    @POST("api/auth/register")
    Call<MessageResponse> register(@Body RegisterRequest body);

    // ── Events ────────────────────────────────────────────────────────
    @GET("api/events")
    Call<List<EventResponse>> getEvents(
            @Query("startTime") long startTime,
            @Query("endTime")   long endTime);

    @GET("api/events")
    Call<List<EventResponse>> getAllEvents();

    @POST("api/events")
    Call<EventResponse> createEvent(@Body EventRequest body);

    @PUT("api/events/{id}")
    Call<EventResponse> updateEvent(@Path("id") int id, @Body EventRequest body);

    @DELETE("api/events/{id}")
    Call<MessageResponse> deleteEvent(@Path("id") int id);

    // ── User ──────────────────────────────────────────────────────────
    @GET("api/users/me")
    Call<UserResponse> getMe();

    @PUT("api/users/me")
    Call<MessageResponse> updateMe(@Body UserResponse body);
}
