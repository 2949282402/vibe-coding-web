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
            String sessionId,
            String searchMode
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
            boolean strictCitation,
            String searchMode
    ) {
    }

    public record FeedbackRequest(
            @jakarta.validation.constraints.NotNull(message = "messageId must not be null") Long messageId,
            @jakarta.validation.constraints.NotNull(message = "helpful must not be null") Boolean helpful,
            String note,
            @NotBlank(message = "sessionId must not be blank") String sessionId
    ) {
    }

    public record ReplayRequest(
            @jakarta.validation.constraints.NotNull(message = "messageId must not be null") Long messageId,
            @NotBlank(message = "sessionId must not be blank") String sessionId,
            String question,
            @Min(value = 1, message = "topK must be at least 1")
            @Max(value = 8, message = "topK must not exceed 8")
            Integer topK,
            String searchMode
    ) {
    }

    public record Source(
            Long postId,
            String title,
            String slug,
            String excerpt,
            double score,
            int citationIndex,
            String sourceType,
            String url,
            String domain
    ) {
    }

    public record AnswerVariant(
            String question,
            String answer,
            String mode,
            List<Integer> citations,
            List<Source> sources,
            LocalDateTime createdAt
    ) {
    }

    public record ChatMessage(
            Long id,
            String role,
            String content,
            String mode,
            List<Integer> citations,
            List<Source> sources,
            List<AnswerVariant> variants,
            LocalDateTime createdAt,
            Boolean feedbackHelpful,
            String feedbackNote,
            LocalDateTime feedbackAt
    ) {
    }

    public record HistoryResponse(
            String sessionId,
            List<ChatMessage> messages
    ) {
    }

    public record SearchResponse(
            String question,
            String answer,
            List<String> followUpQuestions,
            List<Source> sources
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
