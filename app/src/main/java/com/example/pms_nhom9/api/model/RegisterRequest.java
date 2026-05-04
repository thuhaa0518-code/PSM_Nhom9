package com.example.pms_nhom9.api.model;

public class RegisterRequest {
    public String fullName;
    public String email;
    public String password;
    public String studentId;

    public RegisterRequest(String fullName, String email, String password, String studentId) {
        this.fullName  = fullName;
        this.email     = email;
        this.password  = password;
        this.studentId = studentId;
    }
}
