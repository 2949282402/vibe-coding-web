package com.hejulian.blog.rag.domain.model;

import java.time.LocalDateTime;

public record KnowledgeChunk(
        Long postId,
        String postTitle,
        String postSlug,
        int chunkIndex,
        String content,
        String contentHash,
        LocalDateTime publishedAt,
        double[] embedding,
        String embeddingModel
) {

    public KnowledgeChunk withEmbedding(double[] embeddingValue, String model) {
        return new KnowledgeChunk(postId, postTitle, postSlug, chunkIndex, content, contentHash, publishedAt, embeddingValue, model);
    }
}
