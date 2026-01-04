package com.smartcheck.auth.service;

import com.smartcheck.auth.dto.request.LoginRequest;
import com.smartcheck.auth.dto.request.RegisterRequest;
import com.smartcheck.auth.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse login(LoginRequest request);

    void register(RegisterRequest request);
}
