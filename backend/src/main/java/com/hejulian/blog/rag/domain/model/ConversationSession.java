package com.hejulian.blog.rag.domain.model;

import java.time.LocalDateTime;

public record ConversationSession(
        String sessionId,
        String title,
        String preview,
        int messageCount,
        boolean manualTitle,
        boolean deleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
