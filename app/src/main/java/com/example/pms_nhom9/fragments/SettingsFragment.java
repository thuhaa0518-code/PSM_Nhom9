package com.example.pms_nhom9.fragments;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.pms_nhom9.R;
import com.example.pms_nhom9.activities.LoginActivity;
import com.example.pms_nhom9.database.AppDatabase;
import com.example.pms_nhom9.database.UserDao;
import com.example.pms_nhom9.models.User;

import java.util.concurrent.Executors;

public class SettingsFragment extends Fragment {

    // Profile views
    private TextView tvAvatar, tvUserName, tvUserEmail, tvStudentId;
    private EditText etFullName, etEmail, etPhone, etBirthDate;
    private TextView btnClearName, btnClearEmail, btnClearPhone, btnClearBirth;
    private Button btnEditSave;

    // Stats views
    private TextView tvStatEvents, tvStatDone, tvStatStreak;

    private UserDao userDao;
    private int userId;
    private User currentUser;
    private boolean isEditing = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SharedPreferences prefs =
                requireContext().getSharedPreferences("psm_prefs", 0);
        userId  = prefs.getInt("logged_user_id", -1);
        userDao = AppDatabase.getInstance(requireContext()).userDao();

        // Bind profile views
        tvAvatar      = view.findViewById(R.id.tvAvatar);
        tvUserName    = view.findViewById(R.id.tvUserName);
        tvUserEmail   = view.findViewById(R.id.tvUserEmail);
        tvStudentId   = view.findViewById(R.id.tvStudentId);
        etFullName    = view.findViewById(R.id.etFullName);
        etEmail       = view.findViewById(R.id.etEmail);
        etPhone       = view.findViewById(R.id.etPhone);
        etBirthDate   = view.findViewById(R.id.etBirthDate);
        btnClearName  = view.findViewById(R.id.btnClearName);
        btnClearEmail = view.findViewById(R.id.btnClearEmail);
        btnClearPhone = view.findViewById(R.id.btnClearPhone);
        btnClearBirth = view.findViewById(R.id.btnClearBirth);
        btnEditSave   = view.findViewById(R.id.btnEditSave);
        tvStatEvents  = view.findViewById(R.id.tvStatEvents);
        tvStatDone    = view.findViewById(R.id.tvStatDone);
        tvStatStreak  = view.findViewById(R.id.tvStatStreak);

        loadUser();
        loadStats();

        // Khởi tạo: luôn ở chế độ xem, không cho sửa
        setFieldsEnabled(false);
        setClearButtonsVisible(false);
        btnEditSave.setText("✏ Chỉnh sửa");
        isEditing = false;

        // Toggle chỉnh sửa / lưu
        btnEditSave.setOnClickListener(v -> {
            if (isEditing) saveUser();
            else enterEditMode();
        });
        // Nút xóa từng trường (chỉ hiện khi đang edit)
        btnClearName.setOnClickListener(v -> etFullName.setText(""));
        btnClearEmail.setOnClickListener(v -> etEmail.setText(""));
        btnClearPhone.setOnClickListener(v -> etPhone.setText(""));
        btnClearBirth.setOnClickListener(v -> etBirthDate.setText(""));

        // Đăng xuất
        view.findViewById(R.id.rowLogout).setOnClickListener(v -> logout());
    }

    @Override
    public void onResume() {
        super.onResume();
        // Chỉ reload khi không đang trong chế độ chỉnh sửa
        if (!isEditing) {
            loadUser();
        }
    }

    // ── Load user từ DB ──────────────────────────────────────────────
    private void loadUser() {
        Executors.newSingleThreadExecutor().execute(() -> {
            currentUser = userDao.getUserById(userId);
            if (currentUser == null) return;
            requireActivity().runOnUiThread(() -> {
                String name = currentUser.getFullName();
                tvUserName.setText(name);
                tvUserEmail.setText(currentUser.getEmail());
                tvStudentId.setText("MSV: " +
                        (currentUser.getStudentId() != null ? currentUser.getStudentId() : "—"));
                if (name != null && !name.isEmpty())
                    tvAvatar.setText(String.valueOf(name.charAt(0)).toUpperCase());

                etFullName.setText(name);
                etEmail.setText(currentUser.getEmail());
                etPhone.setText(currentUser.getPhone() != null ? currentUser.getPhone() : "");
                etBirthDate.setText(currentUser.getBirthDate() != null ? currentUser.getBirthDate() : "");
            });
        });
    }

    // ── Load stats (đếm sự kiện) ─────────────────────────────────────
    private void loadStats() {
        Executors.newSingleThreadExecutor().execute(() -> {
            // Lấy tổng sự kiện và số đã hoàn thành
            long now = System.currentTimeMillis();
            long yearAgo = now - 365L * 24 * 60 * 60 * 1000;
            java.util.List<com.example.pms_nhom9.models.Event> all =
                    AppDatabase.getInstance(requireContext())
                            .eventDao().getEventsInRangeSync(userId, yearAgo, now + 365L * 24 * 60 * 60 * 1000);
            int total = all.size();
            long done = 0;
            for (com.example.pms_nhom9.models.Event e : all) if (e.isCompleted()) done++;
            int pct = total > 0 ? (int) (done * 100 / total) : 0;

            int finalPct = pct;
            requireActivity().runOnUiThread(() -> {
                tvStatEvents.setText(String.valueOf(total));
                tvStatDone.setText(finalPct + "%");
            });
        });
    }

    // ── Chế độ chỉnh sửa ─────────────────────────────────────────────
    private void enterEditMode() {
        isEditing = true;
        btnEditSave.setText("💾 Lưu");
        setFieldsEnabled(true);
        setClearButtonsVisible(true);
    }

    // ── Lưu thông tin ────────────────────────────────────────────────
    private void saveUser() {
        String name      = etFullName.getText().toString().trim();
        String email     = etEmail.getText().toString().trim();
        String phone     = etPhone.getText().toString().trim();
        String birthDate = etBirthDate.getText().toString().trim();

        if (name.isEmpty()) {
            etFullName.setError("Vui lòng nhập họ tên");
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            currentUser.setFullName(name);
            currentUser.setEmail(email);
            currentUser.setPhone(phone);
            currentUser.setBirthDate(birthDate);
            userDao.updateUser(currentUser);

            requireContext().getSharedPreferences("psm_prefs", 0)
                    .edit().putString("logged_user_name", name).apply();

            requireActivity().runOnUiThread(() -> {
                isEditing = false;
                btnEditSave.setText("✏ Chỉnh sửa");
                setFieldsEnabled(false);
                setClearButtonsVisible(false);
                tvUserName.setText(name);
                tvUserEmail.setText(email);
                if (!name.isEmpty())
                    tvAvatar.setText(String.valueOf(name.charAt(0)).toUpperCase());
                showSuccessDialog();
            });
        });
    }

    private void setFieldsEnabled(boolean on) {
        etFullName.setEnabled(on);
        etEmail.setEnabled(on);
        etPhone.setEnabled(on);
        etBirthDate.setEnabled(on);
    }

    private void setClearButtonsVisible(boolean on) {
        int vis = on ? View.VISIBLE : View.GONE;
        btnClearName.setVisibility(vis);
        btnClearEmail.setVisibility(vis);
        btnClearPhone.setVisibility(vis);
        btnClearBirth.setVisibility(vis);
    }

    // ── Dialog thành công ─────────────────────────────────────────────
    private void showSuccessDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_success);
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
        new android.os.Handler(android.os.Looper.getMainLooper())
                .postDelayed(dialog::dismiss, 1500);
    }

    // ── Đăng xuất ────────────────────────────────────────────────────
    private void logout() {
        requireContext().getSharedPreferences("psm_prefs", 0).edit().clear().apply();
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
