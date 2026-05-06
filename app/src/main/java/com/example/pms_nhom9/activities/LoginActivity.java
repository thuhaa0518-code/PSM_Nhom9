package com.example.pms_nhom9.activities;

// ============================================================
// FILE: LoginActivity.java
// NGƯỜI PHỤ TRÁCH: UYÊN
// MỤC ĐÍCH: Màn hình đăng nhập của ứng dụng PSM
//
// CHỨC NĂNG CHÍNH:
//   - Cho phép người dùng đăng nhập bằng email HOẶC mã số sinh viên (MSSV)
//   - Validate dữ liệu nhập trước khi gọi API
//   - Gọi API POST /api/auth/login để xác thực
//   - Lưu token + userId vào SharedPreferences sau khi đăng nhập thành công
//   - Đồng bộ dữ liệu từ server về Room DB (SyncManager)
//   - Điều hướng sang MainActivity sau khi sync xong
//
// LUỒNG HOẠT ĐỘNG:
//   Người dùng nhập email/MSSV + mật khẩu
//   → bấm "Đăng nhập"
//   → handleLogin() validate input
//   → gọi API login
//   → nếu thành công: saveLoginState() → SyncManager.syncAll() → mở MainActivity
//   → nếu thất bại: hiện Toast lỗi
// ============================================================

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pms_nhom9.R;
import com.example.pms_nhom9.api.ApiClient;
import com.example.pms_nhom9.api.ApiService;
import com.example.pms_nhom9.api.model.LoginRequest;
import com.example.pms_nhom9.api.model.LoginResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    // ── Khai báo các biến UI ──────────────────────────────────────────
    // Các biến này sẽ được gán giá trị trong onCreate() bằng findViewById()
    private EditText etInput;        // Ô nhập email hoặc MSSV (id: etLoginInput)
    private EditText etPassword;     // Ô nhập mật khẩu, ẩn ký tự (id: etLoginPassword)
    private Button btnLogin;         // Nút "Đăng nhập" (id: btnLogin)
    private Button btnGoRegister;    // Nút "Google" → thực ra mở màn đăng ký (id: btnGoRegister)
    private TextView tvForgotPassword; // Link "Quên mật khẩu?" (id: tvForgotPassword)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Gắn file layout activity_login.xml vào Activity này
        // Tất cả view trong XML sẽ được render lên màn hình
        setContentView(R.layout.activity_login);

        // ── Ánh xạ view từ XML sang biến Java ────────────────────────
        // findViewById() tìm view theo id đã khai báo trong XML
        etInput          = findViewById(R.id.etLoginInput);
        etPassword       = findViewById(R.id.etLoginPassword);
        btnLogin         = findViewById(R.id.btnLogin);
        btnGoRegister    = findViewById(R.id.btnGoRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        // ── Gắn sự kiện click ────────────────────────────────────────
        // Lambda v -> ... là cách viết tắt của new View.OnClickListener()
        btnLogin.setOnClickListener(v -> handleLogin()); // bấm Đăng nhập → gọi handleLogin()

        // Bấm "Quên mật khẩu?" → mở ForgotPasswordActivity
        tvForgotPassword.setOnClickListener(v ->
                startActivity(new Intent(this, ForgotPasswordActivity.class)));

        // Bấm nút "Google" (btnGoRegister) → mở màn hình đăng ký
        btnGoRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }

    // ── Xử lý logic đăng nhập ────────────────────────────────────────
    private void handleLogin() {
        // Lấy text từ ô nhập, .trim() xóa khoảng trắng đầu/cuối
        String input    = etInput.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // ── Validate: kiểm tra dữ liệu trước khi gọi API ─────────────
        // TextUtils.isEmpty() trả về true nếu chuỗi null hoặc rỗng ""
        if (TextUtils.isEmpty(input)) {
            etInput.setError("Vui lòng nhập email hoặc MSSV"); // hiện lỗi ngay trên ô nhập
            return; // dừng không chạy tiếp
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Vui lòng nhập mật khẩu");
            return;
        }

        // ── Disable nút tránh bấm 2 lần khi đang gọi API ─────────────
        btnLogin.setEnabled(false);
        btnLogin.setText("Đang đăng nhập...");

        // ── Gọi API đăng nhập ─────────────────────────────────────────
        // reset() xóa Retrofit instance cũ (có thể có token cũ từ lần login trước)
        ApiClient.reset();
        ApiService api = ApiClient.getService(this); // lấy Retrofit service
        // Gọi POST /api/auth/login với body { email/mssv, password }
        api.login(new LoginRequest(input, password)).enqueue(new Callback<LoginResponse>() {

            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                // Callback này chạy trên Main Thread khi nhận được phản hồi từ server
                btnLogin.setEnabled(true);
                btnLogin.setText("Đăng nhập");

                if (response.isSuccessful() && response.body() != null) {
                    // HTTP 200: đăng nhập thành công
                    LoginResponse body = response.body();

                    if (body.user == null) {
                        // Server trả về 200 nhưng không có data user → lỗi bất thường
                        Toast.makeText(LoginActivity.this,
                                "Lỗi dữ liệu từ server", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    saveLoginState(body); // lưu token + userId vào SharedPreferences
                    Toast.makeText(LoginActivity.this,
                            "Xin chào, " + body.user.fullName + "!", Toast.LENGTH_SHORT).show();

                    // Sync dữ liệu từ server về Room DB trước khi vào app
                    // SyncManager chạy trên background thread, callback chạy trên Main Thread
                    com.example.pms_nhom9.api.SyncManager.syncAll(LoginActivity.this, () -> {
                        // Sau khi sync xong → mở MainActivity
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        // FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK:
                        // xóa toàn bộ back stack → người dùng không thể bấm Back về LoginActivity
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    });
                } else {
                    // HTTP 401 hoặc lỗi khác: sai mật khẩu, không tìm thấy user...
                    Toast.makeText(LoginActivity.this,
                            "Sai thông tin đăng nhập", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                // Không kết nối được server: mất mạng, sai IP, server chưa chạy...
                btnLogin.setEnabled(true);
                btnLogin.setText("Đăng nhập");
                Toast.makeText(LoginActivity.this,
                        "Không kết nối được server: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // ── Lưu trạng thái đăng nhập vào SharedPreferences ───────────────
    // SharedPreferences là bộ nhớ key-value nhỏ, tồn tại kể cả khi đóng app
    private void saveLoginState(LoginResponse body) {
        // reset() để lần sau tạo Retrofit mới có token trong header Authorization
        ApiClient.reset();
        getSharedPreferences("psm_prefs", MODE_PRIVATE).edit()
                .putInt("logged_user_id", body.user.id)         // ID dùng để query Room DB
                .putString("logged_user_name", body.user.fullName) // Tên hiển thị ở HomeFragment
                .putString("auth_token", body.token)             // JWT token gửi kèm mọi API request
                .apply(); // apply() lưu bất đồng bộ (không block UI thread)
    }
}
