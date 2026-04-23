package com.hejulian.blog.agent.entity;

import com.hejulian.blog.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AgentEvalRecord extends BaseEntity {

    private Long taskId;

    private Long userId;

    private Integer scoreOverall;

    private Integer scoreGrounding;

    private Integer scoreHelpfulness;

    private Integer scoreStyle;

    private String issueTypes;

    private String feedbackNote;

    private LocalDateTime createdAt;
}

