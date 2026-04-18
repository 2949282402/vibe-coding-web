package com.hejulian.blog.rag.domain.port;

import com.hejulian.blog.rag.domain.model.KnowledgeChunk;
import com.hejulian.blog.rag.domain.model.ScoredChunk;
import java.util.List;

public interface VectorStore {

    boolean isVectorStoreConfigured();

    boolean isCollectionReady();

    void deleteCollection();

    void recreateCollection(int vectorSize);

    void upsertChunks(List<KnowledgeChunk> chunks);

    void deleteChunksByPostId(Long postId);

    List<ScoredChunk> search(double[] queryVector, int limit, double minScore);
}
