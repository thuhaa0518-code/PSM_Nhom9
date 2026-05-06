package com.example.pms_nhom9.fragments;

// ============================================================
// FILE: SettingsFragment.java
// NGƯỜI PHỤ TRÁCH: QUỲNH
// MỤC ĐÍCH: Màn hình cài đặt - kết hợp hồ sơ + thống kê + cài đặt app + đăng xuất
//
// CHỨC NĂNG CHÍNH:
//   1. HỒ SƠ: xem/sửa thông tin cá nhân (giống ProfileFragment nhưng có thêm nút ✕)
//   2. THỐNG KÊ: hiển thị tổng sự kiện, % hoàn thành, chuỗi ngày
//   3. CÀI ĐẶT: khóa sinh trắc học, đồng bộ đám mây, ngôn ngữ (UI only)
//   4. ĐĂNG XUẤT: xóa SharedPreferences → về LoginActivity
//
// KHÁC BIỆT SO VỚI ProfileFragment:
//   - Có thêm nút ✕ bên cạnh mỗi trường (ẩn khi xem, hiện khi sửa)
//   - Có phần thống kê (loadStats)
//   - Có phần cài đặt app
//   - Có nút Đăng xuất
//
// THỐNG KÊ (loadStats):
//   - Lấy tất cả sự kiện trong 1 năm qua + 1 năm tới
//   - Đếm tổng và số đã hoàn thành
//   - Tính % = (done / total) * 100
//
// ĐĂNG XUẤT:
//   - Xóa toàn bộ SharedPreferences "psm_prefs" (token, userId, tên...)
//   - Mở LoginActivity với FLAG_ACTIVITY_CLEAR_TASK
//   - Người dùng không thể bấm Back để vào lại app
// ============================================================

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

    // ── Biến UI phần hồ sơ ───────────────────────────────────────────
    private TextView tvAvatar;    // Chữ cái đầu tên (id: tvAvatar)
    private TextView tvUserName;  // Tên trên banner (id: tvUserName)
    private TextView tvUserEmail; // Email trên banner (id: tvUserEmail)
    private TextView tvStudentId; // MSSV (id: tvStudentId)
    private EditText etFullName;  // Ô nhập họ tên (id: etFullName)
    private EditText etEmail;     // Ô nhập email (id: etEmail)
    private EditText etPhone;     // Ô nhập điện thoại (id: etPhone)
    private EditText etBirthDate; // Ô nhập ngày sinh (id: etBirthDate)
    // Nút ✕ xóa từng trường (ẩn khi xem, hiện khi sửa)
    private TextView btnClearName;  // (id: btnClearName)
    private TextView btnClearEmail; // (id: btnClearEmail)
    private TextView btnClearPhone; // (id: btnClearPhone)
    private TextView btnClearBirth; // (id: btnClearBirth)
    private Button btnEditSave;     // Nút "✏ Chỉnh sửa" / "💾 Lưu" (id: btnEditSave)

    // ── Biến UI phần thống kê ─────────────────────────────────────────
    private TextView tvStatEvents; // Tổng số sự kiện (id: tvStatEvents)
    private TextView tvStatDone;   // % hoàn thành (id: tvStatDone)
    private TextView tvStatStreak; // Chuỗi ngày (id: tvStatStreak)

    // ── Biến dữ liệu ─────────────────────────────────────────────────
    private UserDao userDao;
    private int userId;
    private User currentUser;
    private boolean isEditing = false; // false=xem, true=đang sửa

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

        SharedPreferences prefs = requireContext().getSharedPreferences("psm_prefs", 0);
        userId  = prefs.getInt("logged_user_id", -1);
        userDao = AppDatabase.getInstance(requireContext()).userDao();

        // ── Ánh xạ view ──────────────────────────────────────────────
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

        loadUser();   // load thông tin user từ Room DB
        loadStats();  // tính và hiển thị thống kê

        // ── Khởi tạo ở chế độ XEM ────────────────────────────────────
        setFieldsEnabled(false);
        setClearButtonsVisible(false); // ẩn tất cả nút ✕
        btnEditSave.setText("✏ Chỉnh sửa");
        isEditing = false;

        // ── Toggle chỉnh sửa / lưu ───────────────────────────────────
        btnEditSave.setOnClickListener(v -> {
            if (isEditing) saveUser();
            else enterEditMode();
        });

        // ── Nút ✕ xóa từng trường (chỉ hoạt động khi đang edit) ──────
        btnClearName.setOnClickListener(v -> etFullName.setText(""));
        btnClearEmail.setOnClickListener(v -> etEmail.setText(""));
        btnClearPhone.setOnClickListener(v -> etPhone.setText(""));
        btnClearBirth.setOnClickListener(v -> etBirthDate.setText(""));

        // ── Nút Đăng xuất ────────────────────────────────────────────
        view.findViewById(R.id.rowLogout).setOnClickListener(v -> logout());
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload user khi quay lại fragment
        // Nhưng KHÔNG reload nếu đang sửa (tránh mất dữ liệu đang nhập)
        if (!isEditing) {
            loadUser();
        }
    }

    // ── Load thông tin user từ Room DB ────────────────────────────────
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
                // Avatar: chữ cái đầu tên
                if (name != null && !name.isEmpty())
                    tvAvatar.setText(String.valueOf(name.charAt(0)).toUpperCase());

                etFullName.setText(name);
                etEmail.setText(currentUser.getEmail());
                etPhone.setText(currentUser.getPhone() != null ? currentUser.getPhone() : "");
                etBirthDate.setText(currentUser.getBirthDate() != null ? currentUser.getBirthDate() : "");
            });
        });
    }

    // ── Tính và hiển thị thống kê sự kiện ────────────────────────────
    private void loadStats() {
        Executors.newSingleThreadExecutor().execute(() -> {
            long now     = System.currentTimeMillis();
            long yearAgo = now - 365L * 24 * 60 * 60 * 1000; // 1 năm trước (ms)

            // Lấy tất cả sự kiện trong khoảng 1 năm qua đến 1 năm tới
            java.util.List<com.example.pms_nhom9.models.Event> all =
                    AppDatabase.getInstance(requireContext())
                            .eventDao().getEventsInRangeSync(userId, yearAgo,
                                    now + 365L * 24 * 60 * 60 * 1000);

            int total = all.size(); // tổng số sự kiện
            long done = 0;
            // Đếm số sự kiện đã hoàn thành (isCompleted = true)
            for (com.example.pms_nhom9.models.Event e : all) if (e.isCompleted()) done++;

            // Tính % hoàn thành: tránh chia cho 0 khi total = 0
            int pct = total > 0 ? (int) (done * 100 / total) : 0;

            final int finalPct = pct;
            requireActivity().runOnUiThread(() -> {
                tvStatEvents.setText(String.valueOf(total)); // "128"
                tvStatDone.setText(finalPct + "%");          // "94%"
                // tvStatStreak: hiện tại để cố định, chưa tính động
            });
        });
    }

    // ── Chuyển sang chế độ chỉnh sửa ─────────────────────────────────
    private void enterEditMode() {
        isEditing = true;
        btnEditSave.setText("💾 Lưu");
        setFieldsEnabled(true);          // mở khóa các ô nhập
        setClearButtonsVisible(true);    // hiện nút ✕ bên cạnh mỗi trường
    }

    // ── Lưu thông tin đã chỉnh sửa ───────────────────────────────────
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
            userDao.updateUser(currentUser); // lưu vào Room DB

            // Cập nhật tên trong SharedPreferences để HomeFragment hiển thị đúng
            requireContext().getSharedPreferences("psm_prefs", 0)
                    .edit().putString("logged_user_name", name).apply();

            requireActivity().runOnUiThread(() -> {
                isEditing = false;
                btnEditSave.setText("✏ Chỉnh sửa");
                setFieldsEnabled(false);
                setClearButtonsVisible(false); // ẩn nút ✕ lại
                tvUserName.setText(name);
                tvUserEmail.setText(email);
                if (!name.isEmpty())
                    tvAvatar.setText(String.valueOf(name.charAt(0)).toUpperCase());
                showSuccessDialog();
            });
        });
    }

    // ── Bật/tắt khả năng nhập liệu ───────────────────────────────────
    private void setFieldsEnabled(boolean on) {
        etFullName.setEnabled(on);
        etEmail.setEnabled(on);
        etPhone.setEnabled(on);
        etBirthDate.setEnabled(on);
    }

    // ── Hiện/ẩn nút ✕ xóa từng trường ───────────────────────────────
    // Chỉ hiện khi đang ở chế độ chỉnh sửa
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
        // Tự đóng sau 1.5 giây
        new android.os.Handler(android.os.Looper.getMainLooper())
                .postDelayed(dialog::dismiss, 1500);
    }

    // ── Đăng xuất ────────────────────────────────────────────────────
    private void logout() {
        // Xóa toàn bộ dữ liệu đăng nhập: token, userId, tên...
        // .clear() xóa tất cả key-value trong SharedPreferences "psm_prefs"
        requireContext().getSharedPreferences("psm_prefs", 0).edit().clear().apply();

        // Mở LoginActivity và xóa toàn bộ back stack
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        // FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK:
        // xóa hết stack → người dùng không thể bấm Back để vào lại app
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
