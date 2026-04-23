package com.hejulian.blog.agent.entity;

import com.hejulian.blog.agent.domain.enums.MemoryScope;
import com.hejulian.blog.agent.domain.enums.MemoryType;
import com.hejulian.blog.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AgentMemory extends BaseEntity {

    private Long userId;

    private MemoryScope memoryScope;

    private String topicKey;

    private MemoryType memoryType;

    private String content;

    private String contentSummary;

    private BigDecimal confidenceScore;

    private String sourceType;

    private Long sourceRefId;

    private Boolean pinned;

    private Boolean deleted;

    private LocalDateTime lastHitAt;
}

