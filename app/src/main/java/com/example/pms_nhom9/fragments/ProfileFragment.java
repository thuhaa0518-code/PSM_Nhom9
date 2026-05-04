package com.example.pms_nhom9.fragments;

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

    private TextView tvAvatar, tvUserName, tvUserEmail, tvStudentId;
    private EditText etFullName, etEmail, etPhone, etBirthDate;
    private Button btnEditSave;

    private UserDao userDao;
    private int userId;
    private User currentUser;
    private boolean isEditing = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SharedPreferences prefs =
                requireContext().getSharedPreferences("psm_prefs", 0);
        userId  = prefs.getInt("logged_user_id", -1);
        userDao = AppDatabase.getInstance(requireContext()).userDao();

        tvAvatar    = view.findViewById(R.id.tvAvatar);
        tvUserName  = view.findViewById(R.id.tvUserName);
        tvUserEmail = view.findViewById(R.id.tvUserEmail);
        tvStudentId = view.findViewById(R.id.tvStudentId);
        etFullName  = view.findViewById(R.id.etFullName);
        etEmail     = view.findViewById(R.id.etEmail);
        etPhone     = view.findViewById(R.id.etPhone);
        etBirthDate = view.findViewById(R.id.etBirthDate);
        btnEditSave = view.findViewById(R.id.btnEditSave);

        loadUser();

        // Khởi tạo: luôn ở chế độ xem, không cho sửa
        setFieldsEnabled(false);
        btnEditSave.setText("✏ Chỉnh sửa");
        isEditing = false;

        btnEditSave.setOnClickListener(v -> {
            if (isEditing) {
                saveUser();
            } else {
                enterEditMode();
            }
        });
    }

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

                // Avatar: chữ cái đầu
                if (name != null && !name.isEmpty()) {
                    tvAvatar.setText(String.valueOf(name.charAt(0)).toUpperCase());
                }

                etFullName.setText(name);
                etEmail.setText(currentUser.getEmail());
                etPhone.setText(currentUser.getPhone() != null ? currentUser.getPhone() : "");
                etBirthDate.setText(currentUser.getBirthDate() != null ? currentUser.getBirthDate() : "");
            });
        });
    }

    private void enterEditMode() {
        isEditing = true;
        btnEditSave.setText("💾 Lưu");
        setFieldsEnabled(true);
    }

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

            // Cập nhật SharedPreferences
            requireContext().getSharedPreferences("psm_prefs", 0)
                    .edit()
                    .putString("logged_user_name", name)
                    .apply();

            requireActivity().runOnUiThread(() -> {
                isEditing = false;
                btnEditSave.setText("✏ Chỉnh sửa");
                setFieldsEnabled(false);
                tvUserName.setText(name);
                tvUserEmail.setText(email);
                if (!name.isEmpty()) tvAvatar.setText(String.valueOf(name.charAt(0)).toUpperCase());
                showSuccessDialog();
            });
        });
    }

    private void setFieldsEnabled(boolean enabled) {
        etFullName.setEnabled(enabled);
        etEmail.setEnabled(enabled);
        etPhone.setEnabled(enabled);
        etBirthDate.setEnabled(enabled);
    }

    private void showSuccessDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_success);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();

        // Tự đóng sau 1.5 giây
        new android.os.Handler(android.os.Looper.getMainLooper())
                .postDelayed(dialog::dismiss, 1500);
    }
}
