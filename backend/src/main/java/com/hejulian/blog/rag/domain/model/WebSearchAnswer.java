package com.hejulian.blog.rag.domain.model;

import java.util.List;

public record WebSearchAnswer(
        String answer,
        List<WebSearchSource> sources
) {
}
