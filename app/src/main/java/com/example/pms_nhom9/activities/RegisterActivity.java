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
import com.example.pms_nhom9.api.model.MessageResponse;
import com.example.pms_nhom9.api.model.RegisterRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private EditText etFullName, etEmail, etPassword, etConfirmPassword;
    private Button btnRegister;
    private TextView tvGoLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etFullName        = findViewById(R.id.etRegFullName);
        etEmail           = findViewById(R.id.etRegEmail);
        etPassword        = findViewById(R.id.etRegPassword);
        etConfirmPassword = findViewById(R.id.etRegConfirmPassword);
        btnRegister       = findViewById(R.id.btnRegister);
        tvGoLogin         = findViewById(R.id.tvGoLogin);

        btnRegister.setOnClickListener(v -> handleRegister());
        tvGoLogin.setOnClickListener(v -> finish());
    }

    private void handleRegister() {
        String fullName = etFullName.getText().toString().trim();
        String email    = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirm  = etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(fullName)) { etFullName.setError("Vui lòng nhập họ tên"); return; }
        if (TextUtils.isEmpty(email) || !email.contains("@")) { etEmail.setError("Email không hợp lệ"); return; }
        if (password.length() < 6) { etPassword.setError("Mật khẩu ít nhất 6 ký tự"); return; }
        if (!password.equals(confirm)) { etConfirmPassword.setError("Mật khẩu xác nhận không khớp"); return; }

        btnRegister.setEnabled(false);
        btnRegister.setText("Đang đăng ký...");

        ApiClient.reset();
        ApiService api = ApiClient.getService(this);
        api.register(new RegisterRequest(fullName, email, password, null))
                .enqueue(new Callback<MessageResponse>() {
                    @Override
                    public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                        btnRegister.setEnabled(true);
                        btnRegister.setText("Đăng Kí →");

                        if (response.isSuccessful()) {
                            Toast.makeText(RegisterActivity.this,
                                    "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                            // Chuyển về Login để đăng nhập
                            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                            finish();
                        } else {
                            String msg = "Đăng ký thất bại";
                            if (response.code() == 400) msg = "Email đã được đăng ký";
                            Toast.makeText(RegisterActivity.this, msg, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<MessageResponse> call, Throwable t) {
                        btnRegister.setEnabled(true);
                        btnRegister.setText("Đăng Kí →");
                        Toast.makeText(RegisterActivity.this,
                                "Không kết nối được server", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
