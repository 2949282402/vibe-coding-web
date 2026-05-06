package com.hejulian.blog.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;

public final class AdminDtos {

    private AdminDtos() {
    }

    public record DashboardResponse(
            long postCount,
            long categoryCount,
            long tagCount,
            long pendingCommentCount,
            long approvedCommentCount,
            RagFeedbackSummary ragFeedback,
            List<RecentPostResponse> recentPosts,
            List<RecentCommentResponse> recentComments
    ) {
    }

    public record RagFeedbackSummary(
            long answerCount,
            long feedbackCount,
            long helpfulCount,
            long needsWorkCount,
            double coverageRate,
            double helpfulRate,
            List<RecentRagFeedbackResponse> recentFeedback
    ) {
    }

    public record RecentRagFeedbackResponse(
            Long id,
            boolean helpful,
            String note,
            String answerPreview,
            String mode,
            String sessionId,
            LocalDateTime feedbackAt
    ) {
    }

    public record RagFeedbackListResponse(
            Long id,
            boolean helpful,
            String note,
            String answerPreview,
            String answerContent,
            String mode,
            String sessionId,
            LocalDateTime createdAt,
            LocalDateTime feedbackAt
    ) {
    }

    public record RecentPostResponse(
            Long id,
            String title,
            LocalDateTime updatedAt
    ) {
    }

    public record RecentCommentResponse(
            Long id,
            String nickname,
            String postTitle,
            LocalDateTime createdAt
    ) {
    }

    public record AdminPostListResponse(
            Long id,
            String title,
            String slug,
            String categoryName,
            List<String> tags,
            String status,
            long viewCount,
            LocalDateTime updatedAt
    ) {
    }

    public record AdminPostDetailResponse(
            Long id,
            String title,
            String slug,
            String summary,
            String coverImage,
            String content,
            String status,
            boolean featured,
            boolean allowComment,
            String categoryName,
            List<String> tags,
            LocalDateTime updatedAt
    ) {
    }

    public record PostSaveRequest(
            Long id,
            @NotBlank(message = "Title must not be blank") String title,
            String slug,
            @NotBlank(message = "Summary must not be blank") String summary,
            String coverImage,
            @NotBlank(message = "Content must not be blank") String content,
            @NotBlank(message = "Status must not be blank") String status,
            boolean featured,
            boolean allowComment,
            List<String> tags
    ) {
    }

    public record ImageUploadResponse(
            String url,
            String filename
    ) {
    }

    public record TaxonomySaveRequest(
            Long id,
            @NotBlank(message = "Name must not be blank") String name,
            String slug,
            String description
    ) {
    }

    public record AdminCommentResponse(
            Long id,
            String nickname,
            String email,
            String content,
            String status,
            String postTitle,
            LocalDateTime createdAt
    ) {
    }

    public record CommentReviewRequest(
            @NotBlank(message = "Status must not be blank") String status
    ) {
    }
}
