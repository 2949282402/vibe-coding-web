package com.hejulian.blog.dto;

import jakarta.validation.constraints.NotBlank;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank(message = "Username must not be blank") String username,
            @NotBlank(message = "Password must not be blank") String password
    ) {
    }

    public record UserProfile(
            Long id,
            String username,
            String displayName,
            String role
    ) {
    }

    public record LoginResponse(
            String token,
            UserProfile user
    ) {
    }
}
