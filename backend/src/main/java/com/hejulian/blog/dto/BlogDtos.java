package com.hejulian.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

public final class BlogDtos {

    private BlogDtos() {
    }

    public record SiteHomeResponse(
            String siteName,
            String heroTitle,
            String heroSubtitle,
            SiteStats stats,
            List<PostSummaryResponse> featuredPosts,
            List<PostSummaryResponse> latestPosts,
            List<CategoryResponse> categories,
            List<TagResponse> tags
    ) {
    }

    public record SiteStats(
            long postCount,
            long categoryCount,
            long tagCount,
            long commentCount
    ) {
    }

    public record PostSummaryResponse(
            Long id,
            String title,
            String slug,
            String summary,
            String coverImage,
            String categoryName,
            String categorySlug,
            List<String> tags,
            long viewCount,
            LocalDateTime publishedAt,
            boolean featured
    ) {
    }

    public record PostDetailResponse(
            Long id,
            String title,
            String slug,
            String summary,
            String coverImage,
            String content,
            String categoryName,
            String categorySlug,
            List<String> tags,
            long viewCount,
            LocalDateTime publishedAt,
            boolean featured,
            boolean allowComment,
            List<CommentResponse> comments
    ) {
    }

    public record CategoryResponse(
            Long id,
            String name,
            String slug,
            String description,
            long postCount
    ) {
    }

    public record TagResponse(
            Long id,
            String name,
            String slug,
            long postCount
    ) {
    }

    public record CommentResponse(
            Long id,
            String nickname,
            String content,
            LocalDateTime createdAt
    ) {
    }

    public record CommentCreateRequest(
            @NotNull(message = "Post id is required") Long postId,
            @NotBlank(message = "Comment content must not be blank") String content
    ) {
    }
}
