package com.hejulian.blog.rag.domain.model;

import com.hejulian.blog.dto.RagDtos;
import java.time.LocalDateTime;
import java.util.List;

public record ChatHistoryMessage(
        Long id,
        String sessionId,
        String role,
        String content,
        String mode,
        List<Integer> citations,
        List<RagDtos.Source> sources,
        LocalDateTime createdAt,
        Boolean feedbackHelpful,
        String feedbackNote,
        LocalDateTime feedbackAt
) {
}
