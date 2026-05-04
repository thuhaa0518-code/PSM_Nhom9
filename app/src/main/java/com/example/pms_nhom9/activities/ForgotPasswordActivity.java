package com.example.pms_nhom9.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.example.pms_nhom9.R;
import com.example.pms_nhom9.database.AppDatabase;
import com.example.pms_nhom9.database.UserDao;
import com.example.pms_nhom9.models.User;

import java.util.Random;
import java.util.concurrent.Executors;

public class ForgotPasswordActivity extends AppCompatActivity {

    // Step views
    private LinearLayout layoutStep1, layoutStep2, layoutStep3;
    private View step1Bar, step2Bar, step3Bar;

    // Step 1
    private EditText etEmailOrMssv;
    private AppCompatButton btnSendOtp;

    // Step 2
    private EditText otp1, otp2, otp3, otp4;
    private TextView tvOtpHint, tvResendTimer;
    private AppCompatButton btnVerifyOtp;
    private CountDownTimer countDownTimer;

    // Step 3
    private EditText etNewPassword, etConfirmPassword;
    private AppCompatButton btnResetPassword;

    private UserDao userDao;
    private String generatedOtp;
    private User foundUser;
    private int currentStep = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        userDao = AppDatabase.getInstance(this).userDao();

        bindViews();
        setupListeners();
    }

    private void bindViews() {
        layoutStep1 = findViewById(R.id.layoutStep1);
        layoutStep2 = findViewById(R.id.layoutStep2);
        layoutStep3 = findViewById(R.id.layoutStep3);
        step1Bar    = findViewById(R.id.step1Bar);
        step2Bar    = findViewById(R.id.step2Bar);
        step3Bar    = findViewById(R.id.step3Bar);

        etEmailOrMssv  = findViewById(R.id.etEmailOrMssv);
        btnSendOtp     = findViewById(R.id.btnSendOtp);

        otp1 = findViewById(R.id.otp1);
        otp2 = findViewById(R.id.otp2);
        otp3 = findViewById(R.id.otp3);
        otp4 = findViewById(R.id.otp4);
        tvOtpHint      = findViewById(R.id.tvOtpHint);
        tvResendTimer  = findViewById(R.id.tvResendTimer);
        btnVerifyOtp   = findViewById(R.id.btnVerifyOtp);

        etNewPassword     = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnResetPassword  = findViewById(R.id.btnResetPassword);
    }

    private void setupListeners() {
        // Nút quay lại
        findViewById(R.id.btnBack).setOnClickListener(v -> {
            if (currentStep > 1) goToStep(currentStep - 1);
            else finish();
        });

        // Bước 1: gửi OTP
        btnSendOtp.setOnClickListener(v -> handleSendOtp());

        // Auto-focus OTP boxes
        setupOtpAutoFocus();

        // Bước 2: xác nhận OTP
        btnVerifyOtp.setOnClickListener(v -> handleVerifyOtp());

        // Bước 3: đặt mật khẩu mới
        btnResetPassword.setOnClickListener(v -> handleResetPassword());
    }

    // ── Bước 1 ────────────────────────────────────────────────────────
    private void handleSendOtp() {
        String input = etEmailOrMssv.getText().toString().trim();
        if (TextUtils.isEmpty(input)) {
            etEmailOrMssv.setError("Vui lòng nhập email hoặc MSSV");
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            // Tìm user theo email hoặc MSSV
            User user = null;
            if (input.contains("@")) {
                // Tìm theo email — dùng query có sẵn
                user = userDao.loginByEmail(input, ""); // sẽ null vì sai pass
                // Tìm trực tiếp theo email
                user = findUserByEmail(input);
            } else {
                user = findUserByStudentId(input);
            }

            final User finalUser = user;
            runOnUiThread(() -> {
                if (finalUser == null) {
                    Toast.makeText(this, "Không tìm thấy tài khoản với thông tin này",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                foundUser = finalUser;
                // Tạo OTP 4 số ngẫu nhiên
                generatedOtp = String.format("%04d", new Random().nextInt(10000));

                // Trong app demo: hiện OTP trực tiếp qua Toast
                Toast.makeText(this,
                        "Mã OTP của bạn: " + generatedOtp + "\n(Demo - không gửi email thật)",
                        Toast.LENGTH_LONG).show();

                tvOtpHint.setText("Mã 4 chữ số đã gửi đến\n" + finalUser.getEmail());
                goToStep(2);
                startResendTimer();
            });
        });
    }

    // ── Bước 2 ────────────────────────────────────────────────────────
    private void handleVerifyOtp() {
        String entered = otp1.getText().toString()
                + otp2.getText().toString()
                + otp3.getText().toString()
                + otp4.getText().toString();

        if (entered.length() < 4) {
            Toast.makeText(this, "Vui lòng nhập đủ 4 chữ số", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!entered.equals(generatedOtp)) {
            Toast.makeText(this, "Mã OTP không đúng, vui lòng thử lại", Toast.LENGTH_SHORT).show();
            return;
        }
        if (countDownTimer != null) countDownTimer.cancel();
        goToStep(3);
    }

    // ── Bước 3 ────────────────────────────────────────────────────────
    private void handleResetPassword() {
        String newPass     = etNewPassword.getText().toString().trim();
        String confirmPass = etConfirmPassword.getText().toString().trim();

        if (newPass.length() < 6) {
            etNewPassword.setError("Mật khẩu phải có ít nhất 6 ký tự");
            return;
        }
        if (!newPass.equals(confirmPass)) {
            etConfirmPassword.setError("Mật khẩu xác nhận không khớp");
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            foundUser.setPassword(newPass);
            userDao.updateUser(foundUser);
            runOnUiThread(() -> {
                Toast.makeText(this, "Đặt lại mật khẩu thành công!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            });
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────
    private void goToStep(int step) {
        currentStep = step;
        layoutStep1.setVisibility(step == 1 ? View.VISIBLE : View.GONE);
        layoutStep2.setVisibility(step == 2 ? View.VISIBLE : View.GONE);
        layoutStep3.setVisibility(step == 3 ? View.VISIBLE : View.GONE);

        step1Bar.setBackgroundColor(0xFFA855F7);
        step2Bar.setBackgroundColor(step >= 2 ? 0xFFA855F7 : 0xFFE0D9FF);
        step3Bar.setBackgroundColor(step >= 3 ? 0xFFA855F7 : 0xFFE0D9FF);
    }

    private void startResendTimer() {
        tvResendTimer.setTextColor(0xFF6B7280);
        if (countDownTimer != null) countDownTimer.cancel();
        countDownTimer = new CountDownTimer(60_000, 1000) {
            @Override public void onTick(long ms) {
                tvResendTimer.setText("Gửi lại sau " + (ms / 1000) + "s");
            }
            @Override public void onFinish() {
                tvResendTimer.setText("Gửi lại mã");
                tvResendTimer.setTextColor(0xFFA855F7);
                tvResendTimer.setOnClickListener(v -> handleSendOtp());
            }
        }.start();
    }

    private void setupOtpAutoFocus() {
        EditText[] boxes = {otp1, otp2, otp3, otp4};
        for (int i = 0; i < boxes.length; i++) {
            final int idx = i;
            boxes[i].addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
                @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
                @Override public void afterTextChanged(Editable s) {
                    if (s.length() == 1 && idx < boxes.length - 1) {
                        boxes[idx + 1].requestFocus();
                    }
                }
            });
        }
    }

    private User findUserByEmail(String email) {
        // Dùng countByEmail để check tồn tại, rồi query trực tiếp
        // Vì UserDao không có getByEmail, ta dùng loginByEmail với pass rỗng sẽ null
        // Thêm query mới vào UserDao
        return userDao.getUserByEmail(email);
    }

    private User findUserByStudentId(String studentId) {
        return userDao.getUserByStudentId(studentId);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
    }
}
