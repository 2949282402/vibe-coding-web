package com.hejulian.blog.agent.mapper;

import com.hejulian.blog.agent.domain.enums.EventType;
import com.hejulian.blog.agent.entity.AgentTaskEvent;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface AgentTaskEventMapper {

    int insert(AgentTaskEvent event);

    List<AgentTaskEvent> selectByTaskId(@Param("taskId") Long taskId);

    int insertErrorEvent(
            @Param("taskId") Long taskId,
            @Param("stepId") Long stepId,
            @Param("agentRole") String agentRole,
            @Param("eventType") EventType eventType,
            @Param("payloadSummary") String payloadSummary,
            @Param("status") String status,
            @Param("latencyMs") Long latencyMs,
            @Param("createdAt") LocalDateTime createdAt
    );
}

