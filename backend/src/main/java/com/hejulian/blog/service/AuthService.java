package com.hejulian.blog.service;

import com.hejulian.blog.common.CacheNames;
import com.hejulian.blog.dto.AuthDtos;
import com.hejulian.blog.entity.Role;
import com.hejulian.blog.entity.UserAccount;
import com.hejulian.blog.exception.BusinessException;
import com.hejulian.blog.mapper.UserAccountMapper;
import com.hejulian.blog.rag.application.RagRuntimeContextHolder;
import com.hejulian.blog.rag.infrastructure.client.DashScopeModelGateway;
import com.hejulian.blog.security.AuthenticatedUser;
import com.hejulian.blog.security.JwtTokenProvider;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Locale;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserAccountMapper userAccountMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final DashScopeModelGateway dashScopeModelGateway;
    private final CacheManager cacheManager;

    public AuthDtos.LoginResponse login(AuthDtos.LoginRequest request) {
        String username = normalizeUsername(request.username());
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, request.password())
        );

        var user = userAccountMapper.selectByUsername(username);
        if (user == null) {
            throw new BusinessException("\u8d26\u53f7\u4e0d\u5b58\u5728");
        }

        String token = jwtTokenProvider.generateToken(user.getUsername());
        return new AuthDtos.LoginResponse(token, toUserProfile(user));
    }

    public AuthDtos.LoginResponse register(AuthDtos.RegisterRequest request) {
        String username = normalizeUsername(request.username());
        String email = normalizeEmail(request.email());
        String displayName = normalizeDisplayName(request.displayName());
        String password = String.valueOf(request.password());

        if (userAccountMapper.selectByUsername(username) != null) {
            throw new BusinessException("\u7528\u6237\u540d\u5df2\u5b58\u5728\uff0c\u8bf7\u66f4\u6362\u540e\u91cd\u8bd5");
        }
        if (userAccountMapper.selectByEmail(email) != null) {
            throw new BusinessException("\u90ae\u7bb1\u5df2\u88ab\u6ce8\u518c\uff0c\u8bf7\u76f4\u63a5\u767b\u5f55\u6216\u66f4\u6362\u90ae\u7bb1");
        }
        if (password.length() < 8) {
            throw new BusinessException("\u5bc6\u7801\u957f\u5ea6\u4e0d\u80fd\u5c11\u4e8e 8 \u4f4d");
        }

        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setEmail(email);
        user.setDisplayName(displayName);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(Role.USER);
        user.setQwenApiKey(null);
        user.setQwenChatModel(null);
        user.setQwenWebSearchEnabled(Boolean.FALSE);
        userAccountMapper.insert(user);

        String token = jwtTokenProvider.generateToken(user.getUsername());
        return new AuthDtos.LoginResponse(token, toUserProfile(user));
    }

    public AuthDtos.UserProfile getCurrentProfile(AuthenticatedUser authenticatedUser) {
        UserAccount user = requireCurrentUser(authenticatedUser);
        return toUserProfile(user);
    }

    public AuthDtos.QwenConfigResponse getQwenConfig(AuthenticatedUser authenticatedUser) {
        UserAccount user = requireCurrentUser(authenticatedUser);
        if (!StringUtils.hasText(user.getQwenApiKey())) {
            return new AuthDtos.QwenConfigResponse(false, null, false, List.of());
        }

        String selectedModel = user.getQwenChatModel();
        boolean webSearchEnabled = Boolean.TRUE.equals(user.getQwenWebSearchEnabled());
        List<AuthDtos.QwenModelCapability> models = readCachedCapabilities(user.getQwenApiKey());

        if (!models.isEmpty()) {
            selectedModel = pickSelectedModel(models, selectedModel);
            String resolvedSelectedModel = selectedModel;
            webSearchEnabled = models.stream()
                    .filter(item -> item.model().equals(resolvedSelectedModel))
                    .findFirst()
                    .map(AuthDtos.QwenModelCapability::supportsWebSearch)
                    .orElse(false);
        } else if (StringUtils.hasText(selectedModel)) {
            selectedModel = selectedModel.trim();
            models = List.of(new AuthDtos.QwenModelCapability(selectedModel, webSearchEnabled));
        } else {
            models = resolveCapabilities(user.getQwenApiKey());
            selectedModel = pickSelectedModel(models, selectedModel);
            String resolvedSelectedModel = selectedModel;
            webSearchEnabled = models.stream()
                    .filter(item -> item.model().equals(resolvedSelectedModel))
                    .findFirst()
                    .map(AuthDtos.QwenModelCapability::supportsWebSearch)
                    .orElse(false);
        }

        return new AuthDtos.QwenConfigResponse(
                StringUtils.hasText(user.getQwenApiKey()),
                selectedModel,
                webSearchEnabled,
                models
        );
    }

    public AuthDtos.QwenConfigResponse updateQwenConfig(AuthenticatedUser authenticatedUser, AuthDtos.QwenConfigRequest request) {
        UserAccount user = requireCurrentUser(authenticatedUser);
        String apiKey = StringUtils.hasText(request.apiKey()) ? request.apiKey().trim() : user.getQwenApiKey();
        List<AuthDtos.QwenModelCapability> models = resolveCapabilities(apiKey);
        String selectedModel = pickSelectedModel(models, request.selectedModel());
        boolean webSearchEnabled = models.stream()
                .filter(item -> item.model().equals(selectedModel))
                .findFirst()
                .map(AuthDtos.QwenModelCapability::supportsWebSearch)
                .orElse(false);

        userAccountMapper.updateQwenSettings(user.getId(), apiKey, selectedModel, webSearchEnabled);
        return new AuthDtos.QwenConfigResponse(
                StringUtils.hasText(apiKey),
                selectedModel,
                webSearchEnabled,
                models
        );
    }

    public RagRuntimeContextHolder.RagRuntimeOptions requireRagRuntimeOptions(AuthenticatedUser authenticatedUser) {
        UserAccount user = requireCurrentUser(authenticatedUser);
        return requireRagRuntimeOptions(user);
    }

    public RagRuntimeContextHolder.RagRuntimeOptions requireRagRuntimeOptions(Long userId) {
        UserAccount user = userAccountMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("\u8d26\u53f7\u4e0d\u5b58\u5728");
        }
        return requireRagRuntimeOptions(user);
    }

    private RagRuntimeContextHolder.RagRuntimeOptions requireRagRuntimeOptions(UserAccount user) {
        if (!StringUtils.hasText(user.getQwenApiKey()) || !StringUtils.hasText(user.getQwenChatModel())) {
            throw new BusinessException("\u53d1\u8d77\u5bf9\u8bdd\u524d\u8bf7\u5148\u914d\u7f6e\u5343\u95ee API Key");
        }
        return new RagRuntimeContextHolder.RagRuntimeOptions(
                user.getQwenApiKey().trim(),
                user.getQwenChatModel().trim(),
                Boolean.TRUE.equals(user.getQwenWebSearchEnabled())
        );
    }

    private List<AuthDtos.QwenModelCapability> resolveCapabilities(String apiKey) {
        if (!StringUtils.hasText(apiKey)) {
            return List.of();
        }
        List<AuthDtos.QwenModelCapability> cached = readCachedCapabilities(apiKey);
        if (!cached.isEmpty()) {
            return cached;
        }
        List<AuthDtos.QwenModelCapability> capabilities = dashScopeModelGateway.inspectQwenModels(apiKey.trim());
        if (capabilities.isEmpty()) {
            throw new BusinessException("\u5f53\u524d API Key \u672a\u68c0\u6d4b\u5230\u53ef\u7528\u7684\u5343\u95ee\u5bf9\u8bdd\u6a21\u578b");
        }
        writeCachedCapabilities(apiKey, capabilities);
        return capabilities;
    }

    @SuppressWarnings("unchecked")
    private List<AuthDtos.QwenModelCapability> readCachedCapabilities(String apiKey) {
        if (!StringUtils.hasText(apiKey)) {
            return List.of();
        }
        Cache cache = cacheManager.getCache(CacheNames.QWEN_MODEL_CAPABILITIES);
        if (cache == null) {
            return List.of();
        }
        List<AuthDtos.QwenModelCapability> cached = cache.get(buildQwenCapabilitiesCacheKey(apiKey), List.class);
        return cached == null ? List.of() : List.copyOf(cached);
    }

    private void writeCachedCapabilities(String apiKey, List<AuthDtos.QwenModelCapability> capabilities) {
        if (!StringUtils.hasText(apiKey) || capabilities == null || capabilities.isEmpty()) {
            return;
        }
        Cache cache = cacheManager.getCache(CacheNames.QWEN_MODEL_CAPABILITIES);
        if (cache != null) {
            cache.put(buildQwenCapabilitiesCacheKey(apiKey), List.copyOf(capabilities));
        }
    }

    private String buildQwenCapabilitiesCacheKey(String apiKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(apiKey.trim().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to build Qwen capability cache key", ex);
        }
    }

    private String pickSelectedModel(List<AuthDtos.QwenModelCapability> models, String requestedModel) {
        if (models.isEmpty()) {
            return null;
        }
        if (StringUtils.hasText(requestedModel)) {
            String normalized = requestedModel.trim();
            boolean matched = models.stream().anyMatch(item -> item.model().equals(normalized));
            if (!matched) {
                throw new BusinessException("\u5f53\u524d API Key \u4e0d\u652f\u6301\u6240\u9009\u6a21\u578b");
            }
            return normalized;
        }
        return models.get(0).model();
    }

    private UserAccount requireCurrentUser(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null) {
            throw new BusinessException("\u8bf7\u5148\u767b\u5f55");
        }
        UserAccount user = userAccountMapper.selectById(authenticatedUser.getId());
        if (user == null) {
            throw new BusinessException("\u8d26\u53f7\u4e0d\u5b58\u5728");
        }
        return user;
    }

    private AuthDtos.UserProfile toUserProfile(UserAccount user) {
        return new AuthDtos.UserProfile(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole().name(),
                StringUtils.hasText(user.getQwenApiKey()),
                user.getQwenChatModel(),
                Boolean.TRUE.equals(user.getQwenWebSearchEnabled())
        );
    }

    private String normalizeUsername(String username) {
        String normalized = String.valueOf(username).trim().toLowerCase(Locale.ROOT);
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException("\u8bf7\u8f93\u5165\u7528\u6237\u540d");
        }
        return normalized;
    }

    private String normalizeEmail(String email) {
        String normalized = String.valueOf(email).trim().toLowerCase(Locale.ROOT);
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException("\u8bf7\u8f93\u5165\u90ae\u7bb1");
        }
        return normalized;
    }

    private String normalizeDisplayName(String displayName) {
        String normalized = String.valueOf(displayName).trim();
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException("\u8bf7\u8f93\u5165\u663e\u793a\u540d\u79f0");
        }
        return normalized;
    }
}
