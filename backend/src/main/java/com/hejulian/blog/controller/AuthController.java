package com.hejulian.blog.controller;

import com.hejulian.blog.common.ApiResponse;
import com.hejulian.blog.dto.AuthDtos;
import com.hejulian.blog.security.AuthenticatedUser;
import com.hejulian.blog.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<AuthDtos.LoginResponse> login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        return ApiResponse.success("Login successful", authService.login(request));
    }

    @PostMapping("/register")
    public ApiResponse<AuthDtos.LoginResponse> register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
        return ApiResponse.success("Register successful", authService.register(request));
    }

    @GetMapping("/me")
    public ApiResponse<AuthDtos.UserProfile> me(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return ApiResponse.success(authService.getCurrentProfile(authenticatedUser));
    }

    @GetMapping("/qwen-config")
    public ApiResponse<AuthDtos.QwenConfigResponse> qwenConfig(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ApiResponse.success(authService.getQwenConfig(authenticatedUser));
    }

    @PostMapping("/qwen-config")
    public ApiResponse<AuthDtos.QwenConfigResponse> updateQwenConfig(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestBody AuthDtos.QwenConfigRequest request
    ) {
        return ApiResponse.success(authService.updateQwenConfig(authenticatedUser, request));
    }
}
