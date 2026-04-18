package com.hejulian.blog.rag.domain.model;

import java.time.LocalDateTime;

public record PublishedPost(
        Long id,
        String title,
        String slug,
        String summary,
        String content,
        LocalDateTime publishedAt,
        LocalDateTime updatedAt
) {
}
