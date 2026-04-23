package com.hejulian.blog.agent.entity;

import com.hejulian.blog.agent.domain.enums.ToolPermissionLevel;
import com.hejulian.blog.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AgentToolCall extends BaseEntity {

    private Long taskId;

    private Long stepId;

    private String toolName;

    private ToolPermissionLevel permissionLevel;

    private String requestJson;

    private String responseSummary;

    private Boolean success;

    private String errorMessage;

    private Long latencyMs;

    private LocalDateTime createdAt;
}

