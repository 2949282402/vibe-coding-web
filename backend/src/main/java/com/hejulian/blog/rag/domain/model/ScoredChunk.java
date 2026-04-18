package com.hejulian.blog.rag.domain.model;

public record ScoredChunk(KnowledgeChunk chunk, double score) {
}
