package com.hejulian.blog.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserAccount extends BaseEntity {

    private String username;

    private String email;

    private String password;

    private String displayName;

    private Role role;

    private String qwenApiKey;

    private String qwenChatModel;

    private Boolean qwenWebSearchEnabled;
}
