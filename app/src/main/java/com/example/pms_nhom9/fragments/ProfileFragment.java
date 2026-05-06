package com.example.pms_nhom9.fragments;

// ============================================================
// FILE: ProfileFragment.java
// NGƯỜI PHỤ TRÁCH: QUỲNH
// MỤC ĐÍCH: Màn hình xem và chỉnh sửa thông tin cá nhân
//
// CHỨC NĂNG CHÍNH:
//   - Hiển thị thông tin user: tên, email, MSSV, avatar, phone, ngày sinh
//   - Chế độ XEM (mặc định): các ô EditText bị khóa, không thể nhập
//   - Chế độ SỬA: bấm "✏ Chỉnh sửa" → mở khóa các ô, đổi nút thành "💾 Lưu"
//   - Lưu thông tin: cập nhật Room DB + SharedPreferences
//   - Hiện dialog thành công sau khi lưu (tự đóng sau 1.5 giây)
//
// TRẠNG THÁI:
//   isEditing = false → chế độ XEM (mặc định khi mở fragment)
//   isEditing = true  → chế độ SỬA (sau khi bấm Chỉnh sửa)
//
// LUỒNG HOẠT ĐỘNG:
//   Mở fragment → loadUser() từ Room DB → hiển thị thông tin
//   Bấm "✏ Chỉnh sửa" → enterEditMode() → mở khóa các ô
//   Chỉnh sửa thông tin → bấm "💾 Lưu" → saveUser()
//   → cập nhật Room DB + SharedPreferences → showSuccessDialog()
//
// LƯU Ý:
//   - Tất cả query Room DB chạy trên background thread (Executors)
//   - Cập nhật UI phải chạy trên Main Thread (runOnUiThread)
//   - SharedPreferences "logged_user_name" được cập nhật để HomeFragment
//     hiển thị đúng tên trong lời chào
// ============================================================

import android.app.Dialog;
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
import com.example.pms_nhom9.database.AppDatabase;
import com.example.pms_nhom9.database.UserDao;
import com.example.pms_nhom9.models.User;

import java.util.concurrent.Executors;

public class ProfileFragment extends Fragment {

    // ── Khai báo biến UI ─────────────────────────────────────────────
    private TextView tvAvatar;    // Chữ cái đầu tên, nền cam tròn (id: tvAvatar)
    private TextView tvUserName;  // Tên hiển thị lớn trên banner (id: tvUserName)
    private TextView tvUserEmail; // Email hiển thị trên banner (id: tvUserEmail)
    private TextView tvStudentId; // "MSV: 21110234" (id: tvStudentId)
    private EditText etFullName;  // Ô nhập họ tên (id: etFullName)
    private EditText etEmail;     // Ô nhập email (id: etEmail)
    private EditText etPhone;     // Ô nhập số điện thoại (id: etPhone)
    private EditText etBirthDate; // Ô nhập ngày sinh (id: etBirthDate)
    private Button btnEditSave;   // Nút "✏ Chỉnh sửa" / "💾 Lưu" (id: btnEditSave)

    // ── Biến dữ liệu ─────────────────────────────────────────────────
    private UserDao userDao;      // DAO để query Room DB
    private int userId;           // ID user đang đăng nhập (lấy từ SharedPreferences)
    private User currentUser;     // Object user hiện tại (load từ DB)
    private boolean isEditing = false; // false=xem, true=đang sửa

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate layout fragment_profile.xml
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Lấy userId từ SharedPreferences (đã lưu lúc đăng nhập)
        SharedPreferences prefs = requireContext().getSharedPreferences("psm_prefs", 0);
        userId  = prefs.getInt("logged_user_id", -1); // -1 nếu chưa đăng nhập
        userDao = AppDatabase.getInstance(requireContext()).userDao();

        // ── Ánh xạ view ──────────────────────────────────────────────
        tvAvatar    = view.findViewById(R.id.tvAvatar);
        tvUserName  = view.findViewById(R.id.tvUserName);
        tvUserEmail = view.findViewById(R.id.tvUserEmail);
        tvStudentId = view.findViewById(R.id.tvStudentId);
        etFullName  = view.findViewById(R.id.etFullName);
        etEmail     = view.findViewById(R.id.etEmail);
        etPhone     = view.findViewById(R.id.etPhone);
        etBirthDate = view.findViewById(R.id.etBirthDate);
        btnEditSave = view.findViewById(R.id.btnEditSave);

        loadUser(); // load dữ liệu từ Room DB

        // ── Khởi tạo ở chế độ XEM ────────────────────────────────────
        setFieldsEnabled(false);          // khóa tất cả ô nhập
        btnEditSave.setText("✏ Chỉnh sửa");
        isEditing = false;

        // ── Toggle chỉnh sửa / lưu ───────────────────────────────────
        btnEditSave.setOnClickListener(v -> {
            if (isEditing) {
                saveUser();    // đang sửa → bấm Lưu
            } else {
                enterEditMode(); // đang xem → bấm Chỉnh sửa
            }
        });
    }

    // ── Load thông tin user từ Room DB ────────────────────────────────
    private void loadUser() {
        // Executors.newSingleThreadExecutor(): chạy trên background thread
        // Room DB không cho phép query trên Main Thread (sẽ crash)
        Executors.newSingleThreadExecutor().execute(() -> {
            currentUser = userDao.getUserById(userId); // query Room DB
            if (currentUser == null) return; // user không tồn tại

            // runOnUiThread: cập nhật UI phải chạy trên Main Thread
            requireActivity().runOnUiThread(() -> {
                String name = currentUser.getFullName();
                tvUserName.setText(name);
                tvUserEmail.setText(currentUser.getEmail());
                // Hiện MSSV hoặc "—" nếu không có
                tvStudentId.setText("MSV: " +
                        (currentUser.getStudentId() != null ? currentUser.getStudentId() : "—"));

                // Avatar: lấy chữ cái đầu của tên, viết hoa
                // Ví dụ: "Nguyễn Văn Minh" → "N"
                if (name != null && !name.isEmpty()) {
                    tvAvatar.setText(String.valueOf(name.charAt(0)).toUpperCase());
                }

                // Điền dữ liệu vào các ô EditText
                etFullName.setText(name);
                etEmail.setText(currentUser.getEmail());
                // Dùng "" thay vì null để tránh hiện chữ "null"
                etPhone.setText(currentUser.getPhone() != null ? currentUser.getPhone() : "");
                etBirthDate.setText(currentUser.getBirthDate() != null ? currentUser.getBirthDate() : "");
            });
        });
    }

    // ── Chuyển sang chế độ chỉnh sửa ─────────────────────────────────
    private void enterEditMode() {
        isEditing = true;
        btnEditSave.setText("💾 Lưu"); // đổi text nút
        setFieldsEnabled(true);        // mở khóa tất cả ô nhập
    }

    // ── Lưu thông tin đã chỉnh sửa ───────────────────────────────────
    private void saveUser() {
        // Lấy text từ các ô nhập
        String name      = etFullName.getText().toString().trim();
        String email     = etEmail.getText().toString().trim();
        String phone     = etPhone.getText().toString().trim();
        String birthDate = etBirthDate.getText().toString().trim();

        // Validate: tên không được trống
        if (name.isEmpty()) {
            etFullName.setError("Vui lòng nhập họ tên");
            return;
        }

        // Chạy trên background thread vì Room DB không cho phép trên Main Thread
        Executors.newSingleThreadExecutor().execute(() -> {
            // Cập nhật object user với dữ liệu mới
            currentUser.setFullName(name);
            currentUser.setEmail(email);
            currentUser.setPhone(phone);
            currentUser.setBirthDate(birthDate);
            userDao.updateUser(currentUser); // lưu vào Room DB

            // Cập nhật tên trong SharedPreferences
            // HomeFragment đọc "logged_user_name" để hiển thị lời chào
            requireContext().getSharedPreferences("psm_prefs", 0)
                    .edit()
                    .putString("logged_user_name", name)
                    .apply();

            // Cập nhật UI trên Main Thread
            requireActivity().runOnUiThread(() -> {
                isEditing = false;
                btnEditSave.setText("✏ Chỉnh sửa"); // đổi lại text nút
                setFieldsEnabled(false);             // khóa các ô nhập lại

                // Cập nhật UI ngay lập tức không cần reload
                tvUserName.setText(name);
                tvUserEmail.setText(email);
                // Cập nhật avatar nếu tên thay đổi
                if (!name.isEmpty()) tvAvatar.setText(String.valueOf(name.charAt(0)).toUpperCase());

                showSuccessDialog(); // hiện dialog thành công
            });
        });
    }

    // ── Bật/tắt khả năng nhập liệu của các ô ─────────────────────────
    // enabled=true: ô sáng, có thể nhập
    // enabled=false: ô mờ, chỉ đọc
    private void setFieldsEnabled(boolean enabled) {
        etFullName.setEnabled(enabled);
        etEmail.setEnabled(enabled);
        etPhone.setEnabled(enabled);
        etBirthDate.setEnabled(enabled);
    }

    // ── Hiện dialog thành công ────────────────────────────────────────
    private void showSuccessDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE); // không có title bar
        dialog.setContentView(R.layout.dialog_success);       // layout dialog tùy chỉnh
        if (dialog.getWindow() != null) {
            // Nền trong suốt để thấy bo góc của dialog
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();

        // Tự đóng sau 1500ms (1.5 giây) mà không cần người dùng bấm OK
        new android.os.Handler(android.os.Looper.getMainLooper())
                .postDelayed(dialog::dismiss, 1500);
    }
}
