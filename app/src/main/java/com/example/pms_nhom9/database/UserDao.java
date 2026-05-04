package com.example.pms_nhom9.database;


import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.pms_nhom9.models.User;

@Dao
public interface UserDao {

    // Đăng ký: thêm user mới
    @Insert
    long insertUser(User user);

    // Đăng nhập bằng email + password
    @Query("SELECT * FROM users WHERE email = :email AND password = :password LIMIT 1")
    User loginByEmail(String email, String password);

    // Đăng nhập bằng MSSV + password
    @Query("SELECT * FROM users WHERE studentId = :studentId AND password = :password LIMIT 1")
    User loginByStudentId(String studentId, String password);

    // Kiểm tra email đã tồn tại chưa (tránh trùng)
    @Query("SELECT COUNT(*) FROM users WHERE email = :email")
    int countByEmail(String email);

    // Lấy user theo ID
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    User getUserById(int id);

    // Lấy user theo email (không cần password)
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    User getUserByEmail(String email);

    // Lấy user theo MSSV (không cần password)
    @Query("SELECT * FROM users WHERE studentId = :studentId LIMIT 1")
    User getUserByStudentId(String studentId);

    // Cập nhật thông tin user
    @androidx.room.Update
    void updateUser(User user);
}