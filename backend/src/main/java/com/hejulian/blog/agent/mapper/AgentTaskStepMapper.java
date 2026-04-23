package com.hejulian.blog.agent.mapper;

import com.hejulian.blog.agent.entity.AgentTaskStep;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface AgentTaskStepMapper {

    int insert(AgentTaskStep step);

    int update(
            @Param("step") AgentTaskStep step
    );

    List<AgentTaskStep> selectByTaskId(@Param("taskId") Long taskId);

    int updateExecution(
            @Param("id") Long id,
            @Param("status") String status,
            @Param("outputSummary") String outputSummary,
            @Param("retryCount") Integer retryCount,
            @Param("latencyMs") Long latencyMs,
            @Param("completedAt") LocalDateTime completedAt
    );

    AgentTaskStep selectLatestByTaskId(@Param("taskId") Long taskId);
}

