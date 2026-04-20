package com.hejulian.blog.rag.domain.model;

import jakarta.annotation.Nullable;

public record WebSearchSource(
        int index,
        String title,
        String url,
        @Nullable String siteName,
        @Nullable String icon
) {
}
