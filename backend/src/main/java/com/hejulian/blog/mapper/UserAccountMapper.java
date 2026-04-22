package com.hejulian.blog.mapper;

import com.hejulian.blog.entity.UserAccount;
import org.apache.ibatis.annotations.Param;

public interface UserAccountMapper {

    UserAccount selectByUsername(@Param("username") String username);

    UserAccount selectById(@Param("id") Long id);

    UserAccount selectByEmail(@Param("email") String email);

    long countAll();

    int insert(UserAccount userAccount);

    int updateQwenSettings(
            @Param("id") Long id,
            @Param("qwenApiKey") String qwenApiKey,
            @Param("qwenChatModel") String qwenChatModel,
            @Param("qwenWebSearchEnabled") Boolean qwenWebSearchEnabled
    );
}
