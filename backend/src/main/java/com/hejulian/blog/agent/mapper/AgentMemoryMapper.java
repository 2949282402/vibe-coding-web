package com.hejulian.blog.agent.mapper;

import com.hejulian.blog.agent.entity.AgentMemory;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface AgentMemoryMapper {

    int insert(AgentMemory memory);

    int updatePinned(@Param("id") Long id, @Param("userId") Long userId, @Param("pinned") Boolean pinned);

    int markDeleted(@Param("id") Long id, @Param("userId") Long userId);

    int revokeDelete(@Param("id") Long id, @Param("userId") Long userId);

    AgentMemory selectById(@Param("id") Long id);

    AgentMemory selectByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    List<AgentMemory> selectByUserId(
            @Param("userId") Long userId,
            @Param("memoryScope") String memoryScope,
            @Param("keyword") String keyword,
            @Param("onlyPinned") Boolean onlyPinned,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    long countByUserId(@Param("userId") Long userId, @Param("memoryScope") String memoryScope, @Param("keyword") String keyword, @Param("onlyPinned") Boolean onlyPinned);

    int touchHit(@Param("id") Long id, @Param("lastHitAt") LocalDateTime lastHitAt);

    int upsertByTask(
            @Param("userId") Long userId,
            @Param("taskId") Long taskId,
            @Param("content") String content,
            @Param("summary") String summary
    );
}

