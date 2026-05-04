package com.example.pms_nhom9.activities;

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

    private EditText etInput, etPassword;
    private Button btnLogin, btnGoRegister;
    private TextView tvForgotPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etInput          = findViewById(R.id.etLoginInput);
        etPassword       = findViewById(R.id.etLoginPassword);
        btnLogin         = findViewById(R.id.btnLogin);
        btnGoRegister    = findViewById(R.id.btnGoRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        btnLogin.setOnClickListener(v -> handleLogin());

        tvForgotPassword.setOnClickListener(v ->
                startActivity(new Intent(this, ForgotPasswordActivity.class)));

        btnGoRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void handleLogin() {
        String input    = etInput.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(input)) { etInput.setError("Vui lòng nhập email hoặc MSSV"); return; }
        if (TextUtils.isEmpty(password)) { etPassword.setError("Vui lòng nhập mật khẩu"); return; }

        btnLogin.setEnabled(false);
        btnLogin.setText("Đang đăng nhập...");

        ApiClient.reset(); // reset để tạo client không có token
        ApiService api = ApiClient.getService(this);
        api.login(new LoginRequest(input, password)).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                btnLogin.setEnabled(true);
                btnLogin.setText("Đăng nhập");

                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse body = response.body();
                    if (body.user == null) {
                        Toast.makeText(LoginActivity.this,
                                "Lỗi dữ liệu từ server", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    saveLoginState(body);
                    Toast.makeText(LoginActivity.this,
                            "Xin chào, " + body.user.fullName + "!", Toast.LENGTH_SHORT).show();

                    // Sync dữ liệu từ server về local trước khi vào app
                    com.example.pms_nhom9.api.SyncManager.syncAll(LoginActivity.this, () -> {
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    });
                } else {
                    Toast.makeText(LoginActivity.this,
                            "Sai thông tin đăng nhập", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                btnLogin.setEnabled(true);
                btnLogin.setText("Đăng nhập");
                Toast.makeText(LoginActivity.this,
                        "Không kết nối được server: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void saveLoginState(LoginResponse body) {
        ApiClient.reset(); // reset để lần sau dùng token mới
        getSharedPreferences("psm_prefs", MODE_PRIVATE).edit()
                .putInt("logged_user_id", body.user.id)
                .putString("logged_user_name", body.user.fullName)
                .putString("auth_token", body.token)
                .apply();
    }
}
