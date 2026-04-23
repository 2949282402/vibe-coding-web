package com.hejulian.blog.agent.entity;

import com.hejulian.blog.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AgentMemoryHit extends BaseEntity {

    private Long taskId;

    private Long memoryId;

    private String hitReason;

    private String usedInStep;

    private LocalDateTime createdAt;
}

