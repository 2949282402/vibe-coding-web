package com.hejulian.blog.entity;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RagChatSession {

    private String sessionId;
    private String title;
    private String preview;
    private Integer messageCount;
    private Boolean manualTitle;
    private Boolean deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
