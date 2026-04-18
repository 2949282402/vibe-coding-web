package com.hejulian.blog.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;

public final class RagDtos {

    private RagDtos() {
    }

    public record AskRequest(
            @NotBlank(message = "Question must not be blank") String question,
            @Min(value = 1, message = "topK must be at least 1")
            @Max(value = 8, message = "topK must not exceed 8")
            Integer topK,
            String sessionId
    ) {
    }

    public record AskResponse(
            String sessionId,
            String question,
            String answer,
            String mode,
            boolean llmEnabled,
            int indexedPosts,
            int indexedChunks,
            List<String> followUpQuestions,
            List<Source> sources,
            List<ChatMessage> history,
            boolean strictCitation
    ) {
    }

    public record Source(
            Long postId,
            String title,
            String slug,
            String excerpt,
            double score,
            int citationIndex
    ) {
    }

    public record ChatMessage(
            Long id,
            String role,
            String content,
            String mode,
            List<Integer> citations,
            LocalDateTime createdAt
    ) {
    }

    public record HistoryResponse(
            String sessionId,
            List<ChatMessage> messages
    ) {
    }

    public record SessionSummary(
            String sessionId,
            String title,
            String preview,
            int messageCount,
            boolean deleted,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record SessionListResponse(
            List<SessionSummary> sessions
    ) {
    }

    public record SessionUpdateRequest(
            @NotBlank(message = "Session title must not be blank") String title
    ) {
    }

    public record StreamEvent(
            String type,
            String delta,
            AskResponse response,
            String message
    ) {
    }
}
