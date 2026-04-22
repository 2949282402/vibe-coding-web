package com.hejulian.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import java.util.List;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank(message = "\u8bf7\u8f93\u5165\u7528\u6237\u540d") String username,
            @NotBlank(message = "\u8bf7\u8f93\u5165\u5bc6\u7801") String password
    ) {
    }

    public record RegisterRequest(
            @NotBlank(message = "\u8bf7\u8f93\u5165\u7528\u6237\u540d") String username,
            @Email(message = "\u90ae\u7bb1\u683c\u5f0f\u4e0d\u6b63\u786e")
            @NotBlank(message = "\u8bf7\u8f93\u5165\u90ae\u7bb1") String email,
            @NotBlank(message = "\u8bf7\u8f93\u5165\u663e\u793a\u540d\u79f0") String displayName,
            @NotBlank(message = "\u8bf7\u8f93\u5165\u5bc6\u7801") String password
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
