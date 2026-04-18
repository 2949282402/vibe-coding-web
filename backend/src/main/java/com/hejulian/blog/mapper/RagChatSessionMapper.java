package com.hejulian.blog.mapper;

import com.hejulian.blog.entity.RagChatSession;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface RagChatSessionMapper {

    RagChatSession selectBySessionId(@Param("sessionId") String sessionId);

    List<RagChatSession> selectSessions(@Param("includeDeleted") boolean includeDeleted, @Param("limit") int limit);

    int insert(RagChatSession session);

    int updateLifecycle(
            @Param("sessionId") String sessionId,
            @Param("title") String title,
            @Param("preview") String preview,
            @Param("messageCount") int messageCount,
            @Param("updatedAt") LocalDateTime updatedAt,
            @Param("deleted") boolean deleted
    );

    int updateTitle(@Param("sessionId") String sessionId, @Param("title") String title);

    int markDeleted(@Param("sessionId") String sessionId, @Param("deleted") boolean deleted);
}
