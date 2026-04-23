package com.hejulian.blog.agent.mapper;

import com.hejulian.blog.agent.domain.enums.TaskStatus;
import com.hejulian.blog.agent.entity.AgentTask;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface AgentTaskMapper {

    long countAll(@Param("userId") Long userId, @Param("status") String status, @Param("keyword") String keyword);

    List<AgentTask> selectAll(
            @Param("userId") Long userId,
            @Param("status") String status,
            @Param("keyword") String keyword,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    AgentTask selectById(@Param("id") Long id);

    AgentTask selectByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    int insert(AgentTask task);

    int update(@Param("task") AgentTask task);

    int updateStatus(
            @Param("id") Long id,
            @Param("status") TaskStatus status,
            @Param("currentStep") Integer currentStep,
            @Param("finalOutputSummary") String finalOutputSummary,
            @Param("errorMessage") String errorMessage,
            @Param("startedAt") LocalDateTime startedAt,
            @Param("completedAt") LocalDateTime completedAt
    );
}

