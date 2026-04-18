package com.hejulian.blog.service;

import com.hejulian.blog.dto.AuthDtos;
import com.hejulian.blog.exception.BusinessException;
import com.hejulian.blog.mapper.UserAccountMapper;
import com.hejulian.blog.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserAccountMapper userAccountMapper;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthDtos.LoginResponse login(AuthDtos.LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        var user = userAccountMapper.selectByUsername(request.username());
        if (user == null) {
            throw new BusinessException("User not found");
        }

        String token = jwtTokenProvider.generateToken(user.getUsername());
        return new AuthDtos.LoginResponse(
                token,
                new AuthDtos.UserProfile(
                        user.getId(),
                        user.getUsername(),
                        user.getDisplayName(),
                        user.getRole().name()
                )
        );
    }
}
