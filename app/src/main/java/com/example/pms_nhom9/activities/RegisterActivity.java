package com.example.pms_nhom9.activities;

// ============================================================
// FILE: RegisterActivity.java
// NGƯỜI PHỤ TRÁCH: UYÊN
// MỤC ĐÍCH: Màn hình đăng ký tài khoản mới
//
// CHỨC NĂNG CHÍNH:
//   - Thu thập thông tin: họ tên, email, mật khẩu, xác nhận mật khẩu
//   - Validate từng trường trước khi gọi API
//   - Gọi API POST /api/auth/register để tạo tài khoản
//   - Sau khi đăng ký thành công → chuyển về LoginActivity
//
// QUY TẮC VALIDATE:
//   - Họ tên: không được trống
//   - Email: phải chứa ký tự "@"
//   - Mật khẩu: tối thiểu 6 ký tự
//   - Xác nhận mật khẩu: phải khớp với mật khẩu
//
// LUỒNG HOẠT ĐỘNG:
//   Người dùng điền form → bấm "Đăng Kí"
//   → handleRegister() validate
//   → gọi API register
//   → thành công: Toast + chuyển về LoginActivity
//   → thất bại (HTTP 400): "Email đã được đăng ký"
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
import com.example.pms_nhom9.api.model.MessageResponse;
import com.example.pms_nhom9.api.model.RegisterRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    // ── Khai báo biến UI ─────────────────────────────────────────────
    private EditText etFullName;        // Ô nhập họ và tên (id: etRegFullName)
    private EditText etEmail;           // Ô nhập email (id: etRegEmail)
    private EditText etPassword;        // Ô nhập mật khẩu, ẩn ký tự (id: etRegPassword)
    private EditText etConfirmPassword; // Ô xác nhận mật khẩu (id: etRegConfirmPassword)
    private Button btnRegister;         // Nút "Đăng Kí" (id: btnRegister)
    private TextView tvGoLogin;         // Link "Đăng nhập" → quay về LoginActivity (id: tvGoLogin)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Gắn layout activity_register.xml
        setContentView(R.layout.activity_register);

        // ── Ánh xạ view từ XML ───────────────────────────────────────
        etFullName        = findViewById(R.id.etRegFullName);
        etEmail           = findViewById(R.id.etRegEmail);
        etPassword        = findViewById(R.id.etRegPassword);
        etConfirmPassword = findViewById(R.id.etRegConfirmPassword);
        btnRegister       = findViewById(R.id.btnRegister);
        tvGoLogin         = findViewById(R.id.tvGoLogin);

        // Bấm "Đăng Kí" → gọi handleRegister()
        btnRegister.setOnClickListener(v -> handleRegister());
        // Bấm "Đăng nhập" → finish() đóng màn này, quay về LoginActivity
        tvGoLogin.setOnClickListener(v -> finish());
    }

    // ── Xử lý logic đăng ký ──────────────────────────────────────────
    private void handleRegister() {
        // Lấy text từ 4 ô nhập, .trim() xóa khoảng trắng
        String fullName = etFullName.getText().toString().trim();
        String email    = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirm  = etConfirmPassword.getText().toString().trim();

        // ── Validate từng trường theo thứ tự ─────────────────────────
        if (TextUtils.isEmpty(fullName)) {
            etFullName.setError("Vui lòng nhập họ tên");
            return;
        }
        if (TextUtils.isEmpty(email) || !email.contains("@")) {
            // email.contains("@"): kiểm tra định dạng email cơ bản
            etEmail.setError("Email không hợp lệ");
            return;
        }
        if (password.length() < 6) {
            // Mật khẩu phải có ít nhất 6 ký tự để đảm bảo bảo mật
            etPassword.setError("Mật khẩu ít nhất 6 ký tự");
            return;
        }
        if (!password.equals(confirm)) {
            // Hai mật khẩu phải giống nhau
            etConfirmPassword.setError("Mật khẩu xác nhận không khớp");
            return;
        }

        // ── Disable nút tránh bấm 2 lần ──────────────────────────────
        btnRegister.setEnabled(false);
        btnRegister.setText("Đang đăng ký...");

        // ── Gọi API đăng ký ──────────────────────────────────────────
        ApiClient.reset(); // xóa Retrofit cũ
        ApiService api = ApiClient.getService(this);
        // RegisterRequest(fullName, email, password, studentId=null)
        // studentId để null vì màn này không có ô nhập MSSV
        api.register(new RegisterRequest(fullName, email, password, null))
                .enqueue(new Callback<MessageResponse>() {
                    @Override
                    public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                        btnRegister.setEnabled(true);
                        btnRegister.setText("Đăng Kí →");

                        if (response.isSuccessful()) {
                            // HTTP 201: tạo tài khoản thành công
                            Toast.makeText(RegisterActivity.this,
                                    "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                            // Chuyển về LoginActivity để đăng nhập
                            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                            // FLAG_ACTIVITY_CLEAR_TOP: nếu LoginActivity đã có trong stack
                            // thì đưa nó lên trên thay vì tạo mới
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                            finish(); // đóng RegisterActivity
                        } else {
                            // HTTP 400: email đã tồn tại trong database
                            String msg = "Đăng ký thất bại";
                            if (response.code() == 400) msg = "Email đã được đăng ký";
                            Toast.makeText(RegisterActivity.this, msg, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<MessageResponse> call, Throwable t) {
                        // Lỗi kết nối: mất mạng, server không chạy...
                        btnRegister.setEnabled(true);
                        btnRegister.setText("Đăng Kí →");
                        Toast.makeText(RegisterActivity.this,
                                "Không kết nối được server", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
