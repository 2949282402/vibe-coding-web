package com.hejulian.blog.mapper;

import com.hejulian.blog.entity.UserAccount;
import org.apache.ibatis.annotations.Param;

public interface UserAccountMapper {

    UserAccount selectByUsername(@Param("username") String username);

    long countAll();

    int insert(UserAccount userAccount);
}

