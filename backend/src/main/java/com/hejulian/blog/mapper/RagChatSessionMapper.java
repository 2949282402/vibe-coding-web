package com.hejulian.blog.mapper;

import com.hejulian.blog.entity.RagChatSession;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface RagChatSessionMapper {

    RagChatSession selectBySessionId(@Param("userId") Long userId, @Param("sessionId") String sessionId);

    List<RagChatSession> selectSessions(@Param("userId") Long userId, @Param("includeDeleted") boolean includeDeleted, @Param("limit") int limit);

    int insert(RagChatSession session);

    int updateLifecycle(
            @Param("userId") Long userId,
            @Param("sessionId") String sessionId,
            @Param("title") String title,
            @Param("preview") String preview,
            @Param("messageCount") int messageCount,
            @Param("updatedAt") LocalDateTime updatedAt,
            @Param("deleted") boolean deleted
    );

    int updateTitle(@Param("userId") Long userId, @Param("sessionId") String sessionId, @Param("title") String title);

    int markDeleted(@Param("userId") Long userId, @Param("sessionId") String sessionId, @Param("deleted") boolean deleted);

    int deleteBySessionId(@Param("userId") Long userId, @Param("sessionId") String sessionId);
}
