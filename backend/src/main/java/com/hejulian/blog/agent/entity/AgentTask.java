package com.hejulian.blog.agent.entity;

import com.hejulian.blog.agent.domain.enums.ExecutionMode;
import com.hejulian.blog.agent.domain.enums.SearchScope;
import com.hejulian.blog.agent.domain.enums.TaskStatus;
import com.hejulian.blog.agent.domain.enums.TaskType;
import com.hejulian.blog.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AgentTask extends BaseEntity {

    private Long userId;

    private String sessionId;

    private TaskType taskType;

    private String title;

    private String goal;

    private TaskStatus status;

    private ExecutionMode executionMode;

    private SearchScope searchScope;

    private Boolean allowDraftWrite;

    private Integer currentStep;

    private String finalOutputSummary;

    private String errorMessage;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;
}

