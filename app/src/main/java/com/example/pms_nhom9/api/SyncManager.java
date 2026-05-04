package com.example.pms_nhom9.api;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.pms_nhom9.api.model.EventResponse;
import com.example.pms_nhom9.api.model.UserResponse;
import com.example.pms_nhom9.database.AppDatabase;
import com.example.pms_nhom9.database.EventDao;
import com.example.pms_nhom9.models.Event;
import com.example.pms_nhom9.models.User;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Đồng bộ dữ liệu từ API server về Room local.
 * Gọi sau khi đăng nhập thành công.
 */
public class SyncManager {

    public interface SyncCallback {
        void onDone();
    }

    /**
     * Sync user info + events từ API về Room.
     */
    public static void syncAll(Context ctx, SyncCallback callback) {
        ApiService api = ApiClient.getService(ctx);
        AppDatabase db = AppDatabase.getInstance(ctx);

        // Sync user info
        api.getMe().enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UserResponse u = response.body();
                    SharedPreferences prefs =
                            ctx.getSharedPreferences("psm_prefs", Context.MODE_PRIVATE);
                    // Cập nhật tên trong prefs
                    prefs.edit().putString("logged_user_name", u.fullName).apply();

                    // Upsert user vào Room
                    Executors.newSingleThreadExecutor().execute(() -> {
                        User existing = db.userDao().getUserById(u.id);
                        if (existing == null) {
                            User newUser = new User(u.fullName, u.email, "", u.studentId);
                            newUser.setId(u.id);
                            newUser.setPhone(u.phone);
                            newUser.setBirthDate(u.birthDate);
                            db.userDao().insertUser(newUser);
                        } else {
                            existing.setFullName(u.fullName);
                            existing.setEmail(u.email);
                            existing.setStudentId(u.studentId);
                            existing.setPhone(u.phone);
                            existing.setBirthDate(u.birthDate);
                            db.userDao().updateUser(existing);
                        }
                    });
                }
            }
            @Override public void onFailure(Call<UserResponse> call, Throwable t) {}
        });

        // Sync events: lấy 1 năm trước đến 1 năm sau
        long now = System.currentTimeMillis();
        long from = now - 365L * 24 * 60 * 60 * 1000;
        long to   = now + 365L * 24 * 60 * 60 * 1000;

        api.getEvents(from, to).enqueue(new Callback<List<EventResponse>>() {
            @Override
            public void onResponse(Call<List<EventResponse>> call,
                                   Response<List<EventResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<EventResponse> apiEvents = response.body();
                    int userId = ctx.getSharedPreferences("psm_prefs", Context.MODE_PRIVATE)
                            .getInt("logged_user_id", -1);

                    Executors.newSingleThreadExecutor().execute(() -> {
                        EventDao eventDao = db.eventDao();

                        // Xóa toàn bộ events cũ của user trước khi insert lại
                        // để tránh dữ liệu cũ bị sai vẫn còn trong Room
                        eventDao.deleteAllByUser(userId);

                        // Dùng insertOrReplace để tránh UNIQUE constraint khi sync đồng thời
                        for (EventResponse er : apiEvents) {
                            Event e = er.toEvent();
                            eventDao.insertOrReplaceEvent(e);
                        }

                        if (callback != null) {
                            android.os.Handler mainHandler =
                                    new android.os.Handler(android.os.Looper.getMainLooper());
                            mainHandler.post(callback::onDone);
                        }
                    });
                } else {
                    if (callback != null) callback.onDone();
                }
            }
            @Override public void onFailure(Call<List<EventResponse>> call, Throwable t) {
                if (callback != null) callback.onDone();
            }
        });
    }
}
