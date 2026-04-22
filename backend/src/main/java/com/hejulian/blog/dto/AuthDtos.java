package com.hejulian.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import java.util.List;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank(message = "Username must not be blank") String username,
            @NotBlank(message = "Password must not be blank") String password
    ) {
    }

    public record RegisterRequest(
            @NotBlank(message = "Username must not be blank") String username,
            @Email(message = "Email format is invalid")
            @NotBlank(message = "Email must not be blank") String email,
            @NotBlank(message = "Display name must not be blank") String displayName,
            @NotBlank(message = "Password must not be blank") String password
    ) {
    }

    public record UserProfile(
            Long id,
            String username,
            String email,
            String displayName,
            String role,
            boolean hasQwenApiKey,
            String qwenChatModel,
            boolean qwenWebSearchEnabled
    ) {
    }

    public record LoginResponse(
            String token,
            UserProfile user
    ) {
    }

    public record QwenModelCapability(
            String model,
            boolean supportsWebSearch
    ) {
    }

    public record QwenConfigRequest(
            String apiKey,
            String selectedModel
    ) {
    }

    public record QwenConfigResponse(
            boolean hasApiKey,
            String selectedModel,
            boolean webSearchEnabled,
            List<QwenModelCapability> models
    ) {
    }
}
