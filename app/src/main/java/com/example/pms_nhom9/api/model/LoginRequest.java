package com.example.pms_nhom9.api.model;

public class LoginRequest {
    public String input;
    public String password;

    public LoginRequest(String input, String password) {
        this.input    = input;
        this.password = password;
    }
}
