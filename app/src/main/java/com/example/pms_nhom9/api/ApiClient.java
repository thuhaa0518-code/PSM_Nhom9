package com.example.pms_nhom9.api;

// ============================================================
// FILE: ApiClient.java
// NGƯỜI PHỤ TRÁCH: LY
// MỤC ĐÍCH: Cấu hình và cung cấp Retrofit instance để gọi API
//
// CHỨC NĂNG CHÍNH:
//   - Tạo Retrofit instance với BASE_URL của server Node.js
//   - Tự động thêm JWT token vào header "Authorization" của mọi request
//   - Singleton pattern: chỉ tạo 1 instance duy nhất (tiết kiệm tài nguyên)
//   - reset(): xóa instance cũ khi logout hoặc token thay đổi
//   - getService(): trả về ApiService interface để gọi các endpoint
//
// CẤU HÌNH:
//   BASE_URL: địa chỉ server Node.js
//   - Emulator Android: dùng 10.0.2.2 (trỏ về localhost máy host)
//   - Điện thoại thật: dùng IP máy tính trong cùng mạng WiFi
//   - Ví dụ: "http://192.168.1.13:3000/"
//
// INTERCEPTOR:
//   Mỗi request HTTP sẽ tự động được thêm:
//   - Header "Authorization: Bearer {token}" → server dùng để xác thực
//   - Header "Content-Type: application/json" → báo server dữ liệu là JSON
//
// LUỒNG SỬ DỤNG:
//   1. Đăng nhập → nhận token → lưu vào SharedPreferences
//   2. ApiClient.reset() → xóa instance cũ
//   3. ApiClient.getService(ctx) → tạo instance mới với token mới
//   4. Gọi API → token tự động được thêm vào header
//   5. Đăng xuất → ApiClient.reset() → xóa instance có token
// ============================================================

import android.content.Context;
import android.content.SharedPreferences;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    // ── Địa chỉ server ────────────────────────────────────────────────
    // ⚠️ QUAN TRỌNG: Đổi IP này khi test trên điện thoại thật
    // - Emulator: dùng "http://10.0.2.2:3000/" (10.0.2.2 = localhost của máy host)
    // - Điện thoại thật: dùng IP máy tính (xem bằng ipconfig/ifconfig)
    //   Ví dụ: "http://192.168.1.13:3000/"
    // - Cả điện thoại và máy tính phải cùng mạng WiFi
    public static final String BASE_URL = "http://192.168.1.13:3000/";

    // Singleton: chỉ có 1 instance Retrofit trong toàn app
    // static → thuộc về class, không phải object
    private static Retrofit retrofit;

    /**
     * Lấy Retrofit instance (tạo mới nếu chưa có).
     * Đọc token từ SharedPreferences và thêm vào header mọi request.
     *
     * @param ctx Context để đọc SharedPreferences
     * @return Retrofit instance đã cấu hình
     */
    public static Retrofit getInstance(Context ctx) {
        if (retrofit == null) {
            // Chỉ tạo mới khi chưa có instance (Singleton pattern)

            // Lấy JWT token đã lưu lúc đăng nhập
            SharedPreferences prefs = ctx.getSharedPreferences("psm_prefs", Context.MODE_PRIVATE);
            String token = prefs.getString("auth_token", ""); // "" nếu chưa đăng nhập

            // ── Logging Interceptor: in log request/response ra Logcat ──
            // Hữu ích khi debug: xem body request, response, headers...
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY); // log toàn bộ body

            // ── OkHttpClient: HTTP client với interceptors ──────────────
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(chain -> {
                        // Interceptor này chạy trước MỌI request HTTP
                        Request original = chain.request(); // request gốc
                        Request request = original.newBuilder()
                                // Thêm JWT token vào header Authorization
                                // Server sẽ đọc header này để xác thực người dùng
                                .header("Authorization", "Bearer " + token)
                                // Báo server dữ liệu gửi lên là JSON
                                .header("Content-Type", "application/json")
                                .build();
                        return chain.proceed(request); // tiếp tục gửi request đã sửa
                    })
                    .addInterceptor(logging) // thêm logging sau auth interceptor
                    .build();

            // ── Tạo Retrofit instance ────────────────────────────────────
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)                          // địa chỉ server
                    .client(client)                             // dùng OkHttpClient đã cấu hình
                    .addConverterFactory(GsonConverterFactory.create()) // tự động parse JSON ↔ Java object
                    .build();
        }
        return retrofit;
    }

    /**
     * Xóa Retrofit instance hiện tại.
     * Phải gọi khi:
     * - Đăng xuất (xóa instance có token cũ)
     * - Đăng nhập thành công (xóa instance không có token, tạo lại với token mới)
     * - Token hết hạn (xóa để tạo lại với token mới)
     */
    public static void reset() {
        retrofit = null; // đặt về null → lần sau gọi getInstance() sẽ tạo mới
    }

    /**
     * Lấy ApiService interface để gọi các endpoint.
     * Retrofit tự tạo implementation từ interface ApiService.
     *
     * @param ctx Context để đọc token
     * @return ApiService đã cấu hình với token
     */
    public static ApiService getService(Context ctx) {
        // create() tạo implementation của ApiService dựa trên annotations @GET, @POST...
        return getInstance(ctx).create(ApiService.class);
    }
}
