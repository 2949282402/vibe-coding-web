package com.hejulian.blog.agent.entity;

import com.hejulian.blog.agent.domain.enums.AgentRole;
import com.hejulian.blog.agent.domain.enums.EventType;
import com.hejulian.blog.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AgentTaskEvent extends BaseEntity {

    private Long taskId;

    private Long stepId;

    private EventType eventType;

    private AgentRole agentRole;

    private String payloadJson;

    private String payloadSummary;

    private String status;

    private Long latencyMs;

    private LocalDateTime createdAt;
}

