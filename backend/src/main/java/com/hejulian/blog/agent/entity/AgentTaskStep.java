package com.hejulian.blog.agent.entity;

import com.hejulian.blog.agent.domain.enums.AgentRole;
import com.hejulian.blog.agent.domain.enums.TaskStatus;
import com.hejulian.blog.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AgentTaskStep extends BaseEntity {

    private Long taskId;

    private Integer stepIndex;

    private AgentRole agentRole;

    private String stepName;

    private TaskStatus status;

    private String inputSummary;

    private String outputSummary;

    private Integer retryCount;

    private Long latencyMs;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;
}

