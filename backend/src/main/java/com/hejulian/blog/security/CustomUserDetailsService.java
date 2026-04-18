package com.hejulian.blog.security;

import com.hejulian.blog.mapper.UserAccountMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserAccountMapper userAccountMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var user = userAccountMapper.selectByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }
        return new AuthenticatedUser(user);
    }
}
