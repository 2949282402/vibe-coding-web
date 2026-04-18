package com.hejulian.blog.rag.domain.model;

import java.time.LocalDateTime;
import java.util.List;

public record ChatHistoryMessage(
        Long id,
        String sessionId,
        String role,
        String content,
        String mode,
        List<Integer> citations,
        LocalDateTime createdAt
) {
}
