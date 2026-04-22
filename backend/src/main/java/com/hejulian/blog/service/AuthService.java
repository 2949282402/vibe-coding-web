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
            throw new BusinessException("User not found");
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
            throw new BusinessException("Username already exists");
        }
        if (userAccountMapper.selectByEmail(email) != null) {
            throw new BusinessException("Email already exists");
        }
        if (password.length() < 8) {
            throw new BusinessException("Password must be at least 8 characters");
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
        if (!StringUtils.hasText(user.getQwenApiKey()) || !StringUtils.hasText(user.getQwenChatModel())) {
            throw new BusinessException("Please configure your Qwen API Key before starting a conversation");
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
            throw new BusinessException("No available Qwen chat model was detected for this API key");
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
                throw new BusinessException("Selected model is not available for this API key");
            }
            return normalized;
        }
        return models.get(0).model();
    }

    private UserAccount requireCurrentUser(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null) {
            throw new BusinessException("Login required");
        }
        UserAccount user = userAccountMapper.selectById(authenticatedUser.getId());
        if (user == null) {
            throw new BusinessException("User not found");
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
            throw new BusinessException("Username must not be blank");
        }
        return normalized;
    }

    private String normalizeEmail(String email) {
        String normalized = String.valueOf(email).trim().toLowerCase(Locale.ROOT);
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException("Email must not be blank");
        }
        return normalized;
    }

    private String normalizeDisplayName(String displayName) {
        String normalized = String.valueOf(displayName).trim();
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException("Display name must not be blank");
        }
        return normalized;
    }
}
