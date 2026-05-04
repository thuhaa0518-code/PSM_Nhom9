package com.example.pms_nhom9.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class User {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String fullName;
    private String email;
    private String password;
    private String studentId;
    private String phone;      // số điện thoại
    private String birthDate;  // ngày sinh dạng "dd/MM/yyyy"

    // Constructor
    public User(String fullName, String email, String password, String studentId) {
        this.fullName  = fullName;
        this.email     = email;
        this.password  = password;
        this.studentId = studentId;
    }

    // --- Getters & Setters ---
    public int getId()                   { return id; }
    public void setId(int id)            { this.id = id; }

    public String getFullName()          { return fullName; }
    public void setFullName(String v)    { this.fullName = v; }

    public String getEmail()             { return email; }
    public void setEmail(String v)       { this.email = v; }

    public String getPassword()          { return password; }
    public void setPassword(String v)    { this.password = v; }

    public String getStudentId()         { return studentId; }
    public void setStudentId(String v)   { this.studentId = v; }

    public String getPhone()             { return phone; }
    public void setPhone(String v)       { this.phone = v; }

    public String getBirthDate()         { return birthDate; }
    public void setBirthDate(String v)   { this.birthDate = v; }
}