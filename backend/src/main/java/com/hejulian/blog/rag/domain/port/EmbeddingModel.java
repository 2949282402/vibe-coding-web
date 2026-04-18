package com.hejulian.blog.rag.domain.port;

import jakarta.annotation.Nullable;
import java.util.List;

public interface EmbeddingModel {

    boolean isEmbeddingConfigured();

    String embeddingModelName();

    @Nullable
    default double[] embedQuery(String query) {
        List<double[]> embeddings = embedDocuments(List.of(query));
        return embeddings.isEmpty() ? null : embeddings.get(0);
    }

    List<double[]> embedDocuments(List<String> texts);
}
