package com.hejulian.blog.rag.domain.model;

import jakarta.annotation.Nullable;

public record RagEvidence(
        String sourceType,
        String title,
        String reference,
        String content,
        double score,
        int citationIndex,
        @Nullable Long postId,
        @Nullable String slug,
        @Nullable String url,
        @Nullable String domain
) {
}
