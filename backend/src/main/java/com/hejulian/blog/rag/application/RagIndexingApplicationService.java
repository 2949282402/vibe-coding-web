package com.hejulian.blog.rag.application;

import com.hejulian.blog.entity.Post;
import com.hejulian.blog.entity.PostStatus;
import com.hejulian.blog.rag.config.RagProperties;
import com.hejulian.blog.rag.domain.model.IndexState;
import com.hejulian.blog.rag.domain.model.KnowledgeChunk;
import com.hejulian.blog.rag.domain.model.PublishedPost;
import com.hejulian.blog.rag.domain.port.EmbeddingModel;
import com.hejulian.blog.rag.domain.port.KnowledgeBaseRepository;
import com.hejulian.blog.rag.domain.port.VectorStore;
import com.hejulian.blog.rag.domain.service.KnowledgeRetrievalService;
import com.hejulian.blog.rag.domain.service.KnowledgeTextProcessor;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagIndexingApplicationService {

    private final RagProperties ragProperties;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final EmbeddingModel embeddingModel;
    private final VectorStore vectorStore;
    private final KnowledgeTextProcessor textProcessor;
    private final KnowledgeRetrievalService retrievalService;

    @Transactional
    public IndexState ensureIndexReady() {
        List<PublishedPost> publishedPosts = knowledgeBaseRepository.loadPublishedPosts();
        String currentFingerprint = textProcessor.buildFingerprint(publishedPosts);
        long chunkCount = knowledgeBaseRepository.countChunks();
        String cachedFingerprint = knowledgeBaseRepository.readFingerprint();
        boolean vectorStoreMissing = vectorStore.isVectorStoreConfigured() && !vectorStore.isCollectionReady();

        if (chunkCount == 0 || vectorStoreMissing || !currentFingerprint.equals(cachedFingerprint)) {
            rebuildIndexInternal(publishedPosts, currentFingerprint);
        }

        return new IndexState(publishedPosts.size(), (int) knowledgeBaseRepository.countChunks(), currentFingerprint);
    }

    @Transactional
    public int rebuildIndex() {
        List<PublishedPost> publishedPosts = knowledgeBaseRepository.loadPublishedPosts();
        rebuildIndexInternal(publishedPosts, textProcessor.buildFingerprint(publishedPosts));
        return (int) knowledgeBaseRepository.countChunks();
    }

    @Transactional
    public void syncPost(Post post) {
        if (post == null || post.getId() == null) {
            return;
        }

        if (post.getStatus() != PostStatus.PUBLISHED) {
            removePost(post.getId());
            return;
        }

        List<KnowledgeChunk> chunks = textProcessor.buildChunks(
                List.of(toPublishedPost(post)),
                ragProperties.getChunkSize(),
                ragProperties.getChunkOverlap()
        );
        List<KnowledgeChunk> enrichedChunks = enrichEmbeddings(chunks);

        knowledgeBaseRepository.replaceChunksByPostId(post.getId(), enrichedChunks);

        if (vectorStore.isVectorStoreConfigured() && !enrichedChunks.isEmpty()) {
            if (!vectorStore.isCollectionReady()) {
                vectorStore.recreateCollection(enrichedChunks.getFirst().embedding().length);
            }
            vectorStore.deleteChunksByPostId(post.getId());
            vectorStore.upsertChunks(enrichedChunks);
        }

        refreshFingerprint();
    }

    @Transactional
    public void removePost(Long postId) {
        if (postId == null) {
            return;
        }
        knowledgeBaseRepository.deleteChunksByPostId(postId);
        if (vectorStore.isVectorStoreConfigured()) {
            vectorStore.deleteChunksByPostId(postId);
        }
        refreshFingerprint();
    }

    private void rebuildIndexInternal(List<PublishedPost> publishedPosts, String fingerprint) {
        List<KnowledgeChunk> chunks = textProcessor.buildChunks(
                publishedPosts,
                ragProperties.getChunkSize(),
                ragProperties.getChunkOverlap()
        );
        List<KnowledgeChunk> enrichedChunks = enrichEmbeddings(chunks);

        if (vectorStore.isVectorStoreConfigured()) {
            if (enrichedChunks.isEmpty()) {
                vectorStore.deleteCollection();
            } else {
                int vectorSize = enrichedChunks.stream()
                        .map(KnowledgeChunk::embedding)
                        .filter(vector -> vector != null && vector.length > 0)
                        .findFirst()
                        .map(vector -> vector.length)
                        .orElse(0);
                vectorStore.recreateCollection(vectorSize);
                vectorStore.upsertChunks(enrichedChunks);
            }
        }

        knowledgeBaseRepository.replaceChunks(enrichedChunks, fingerprint);
    }

    private void refreshFingerprint() {
        List<PublishedPost> publishedPosts = knowledgeBaseRepository.loadPublishedPosts();
        knowledgeBaseRepository.writeFingerprint(textProcessor.buildFingerprint(publishedPosts));
    }

    private List<KnowledgeChunk> enrichEmbeddings(List<KnowledgeChunk> chunks) {
        if (!embeddingModel.isEmbeddingConfigured() || chunks.isEmpty()) {
            return chunks;
        }

        List<String> embeddingTexts = chunks.stream()
                .map(retrievalService::buildEmbeddingText)
                .toList();
        List<double[]> embeddings = embeddingModel.embedDocuments(embeddingTexts);
        if (embeddings.size() != chunks.size()) {
            throw new IllegalStateException("Embedding count mismatch. expected=" + chunks.size() + ", actual=" + embeddings.size());
        }

        List<KnowledgeChunk> enriched = new ArrayList<>(chunks.size());
        for (int index = 0; index < chunks.size(); index++) {
            double[] vector = embeddings.get(index);
            if (vector == null || vector.length == 0) {
                throw new IllegalStateException("Embedding vector is empty for chunk index " + index);
            }
            enriched.add(chunks.get(index).withEmbedding(vector, embeddingModel.embeddingModelName()));
        }
        return enriched;
    }

    private PublishedPost toPublishedPost(Post post) {
        return new PublishedPost(
                post.getId(),
                post.getTitle(),
                post.getSlug(),
                post.getSummary(),
                post.getContent(),
                post.getPublishedAt(),
                post.getUpdatedAt()
        );
    }
}
