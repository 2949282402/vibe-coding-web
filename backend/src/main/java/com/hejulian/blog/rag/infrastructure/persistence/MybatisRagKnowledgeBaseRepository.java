package com.hejulian.blog.rag.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hejulian.blog.common.CacheNames;
import com.hejulian.blog.entity.Post;
import com.hejulian.blog.entity.RagChatMessage;
import com.hejulian.blog.entity.RagChatSession;
import com.hejulian.blog.entity.PostStatus;
import com.hejulian.blog.entity.RagChunk;
import com.hejulian.blog.mapper.PostMapper;
import com.hejulian.blog.mapper.RagChatMessageMapper;
import com.hejulian.blog.mapper.RagChatSessionMapper;
import com.hejulian.blog.mapper.RagChunkMapper;
import com.hejulian.blog.rag.domain.model.ChatHistoryMessage;
import com.hejulian.blog.rag.domain.model.ConversationSession;
import com.hejulian.blog.rag.domain.model.KnowledgeChunk;
import com.hejulian.blog.rag.domain.model.PublishedPost;
import com.hejulian.blog.rag.domain.port.KnowledgeBaseRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
@RequiredArgsConstructor
@Slf4j
public class MybatisRagKnowledgeBaseRepository implements KnowledgeBaseRepository {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final PostMapper postMapper;
    private final RagChunkMapper ragChunkMapper;
    private final RagChatMessageMapper ragChatMessageMapper;
    private final RagChatSessionMapper ragChatSessionMapper;
    private final CacheManager cacheManager;

    @Override
    public List<PublishedPost> loadPublishedPosts() {
        return postMapper.selectAllPublishedForRag(PostStatus.PUBLISHED.name()).stream()
                .map(this::toPublishedPost)
                .toList();
    }

    @Override
    public List<KnowledgeChunk> loadChunks() {
        return ragChunkMapper.selectAll().stream()
                .map(this::toKnowledgeChunk)
                .toList();
    }

    @Override
    public long countChunks() {
        return ragChunkMapper.countAll();
    }

    @Override
    public long countChunksWithoutEmbedding() {
        return ragChunkMapper.countWithoutEmbedding();
    }

    @Override
    public String readFingerprint() {
        Cache cache = cacheManager.getCache(CacheNames.RAG_META);
        return cache == null ? null : cache.get("index-fingerprint", String.class);
    }

    @Override
    public void writeFingerprint(String fingerprint) {
        writeFingerprintCache(fingerprint);
    }

    @Override
    public void replaceChunks(List<KnowledgeChunk> chunks, String fingerprint) {
        ragChunkMapper.deleteAll();
        if (!chunks.isEmpty()) {
            ragChunkMapper.batchInsert(chunks.stream().map(this::toEntity).toList());
        }
        writeFingerprintCache(fingerprint);
        clearCache(CacheNames.RAG_ANSWER);
    }

    @Override
    public void replaceChunksByPostId(Long postId, List<KnowledgeChunk> chunks) {
        ragChunkMapper.deleteByPostId(postId);
        if (!chunks.isEmpty()) {
            ragChunkMapper.batchInsert(chunks.stream().map(this::toEntity).toList());
        }
        clearCache(CacheNames.RAG_ANSWER);
    }

    @Override
    public void deleteChunksByPostId(Long postId) {
        ragChunkMapper.deleteByPostId(postId);
        clearCache(CacheNames.RAG_ANSWER);
    }

    @Override
    public List<ChatHistoryMessage> loadConversationHistory(Long userId, String sessionId) {
        return ragChatMessageMapper.selectBySessionId(userId, sessionId).stream()
                .map(this::toHistoryMessage)
                .toList();
    }

    @Override
    public ChatHistoryMessage saveConversationMessage(Long userId, ChatHistoryMessage message) {
        RagChatMessage entity = toEntity(userId, message);
        ragChatMessageMapper.insert(entity);
        return new ChatHistoryMessage(
                entity.getId(),
                entity.getSessionId(),
                entity.getRole(),
                entity.getContent(),
                entity.getAnswerMode(),
                readCitations(entity.getCitationsJson()),
                readSources(entity.getSourcesJson()),
                readVariants(entity.getVariantsJson()),
                entity.getCreatedAt(),
                entity.getFeedbackHelpful(),
                entity.getFeedbackNote(),
                entity.getFeedbackAt()
        );
    }

    @Override
    public ChatHistoryMessage updateConversationFeedback(Long userId, String sessionId, Long messageId, Boolean helpful, String note) {
        ragChatMessageMapper.updateFeedback(userId, sessionId, messageId, helpful, note);
        return ragChatMessageMapper.selectBySessionId(userId, sessionId).stream()
                .filter(message -> message.getId().equals(messageId))
                .findFirst()
                .map(this::toHistoryMessage)
                .orElse(null);
    }

    @Override
    public ChatHistoryMessage updateConversationVariants(Long userId, String sessionId, Long messageId, List<com.hejulian.blog.dto.RagDtos.AnswerVariant> variants) {
        ragChatMessageMapper.updateVariants(userId, sessionId, messageId, writeVariants(variants));
        return ragChatMessageMapper.selectBySessionId(userId, sessionId).stream()
                .filter(message -> message.getId().equals(messageId))
                .findFirst()
                .map(this::toHistoryMessage)
                .orElse(null);
    }

    @Override
    public void deleteConversationMessagesFrom(Long userId, String sessionId, Long fromMessageId) {
        ragChatMessageMapper.deleteBySessionIdFromMessageId(userId, sessionId, fromMessageId);
    }

    @Override
    public ConversationSession findConversationSession(Long userId, String sessionId) {
        return toConversationSession(ragChatSessionMapper.selectBySessionId(userId, sessionId));
    }

    @Override
    public List<ConversationSession> listConversationSessions(Long userId, boolean includeDeleted, int limit) {
        return ragChatSessionMapper.selectSessions(userId, includeDeleted, limit).stream()
                .map(this::toConversationSession)
                .toList();
    }

    @Override
    public ConversationSession saveOrUpdateConversationSession(Long userId, String sessionId, String generatedTitle, String preview, int messageCount) {
        RagChatSession existing = ragChatSessionMapper.selectBySessionId(userId, sessionId);
        LocalDateTime now = LocalDateTime.now();

        if (existing == null) {
            RagChatSession session = new RagChatSession();
            session.setSessionId(sessionId);
            session.setUserId(userId);
            session.setTitle(generatedTitle);
            session.setPreview(preview);
            session.setMessageCount(messageCount);
            session.setManualTitle(Boolean.FALSE);
            session.setDeleted(Boolean.FALSE);
            session.setCreatedAt(now);
            session.setUpdatedAt(now);
            ragChatSessionMapper.insert(session);
            return toConversationSession(session);
        }

        String title = Boolean.TRUE.equals(existing.getManualTitle()) ? existing.getTitle() : generatedTitle;
        ragChatSessionMapper.updateLifecycle(userId, sessionId, title, preview, messageCount, now, false);
        existing.setTitle(title);
        existing.setPreview(preview);
        existing.setMessageCount(messageCount);
        existing.setDeleted(Boolean.FALSE);
        existing.setUpdatedAt(now);
        return toConversationSession(existing);
    }

    @Override
    public ConversationSession renameConversationSession(Long userId, String sessionId, String title) {
        ragChatSessionMapper.updateTitle(userId, sessionId, title);
        return findConversationSession(userId, sessionId);
    }

    @Override
    public void markConversationSessionDeleted(Long userId, String sessionId, boolean deleted) {
        ragChatSessionMapper.markDeleted(userId, sessionId, deleted);
    }

    @Override
    public void deleteConversationSessionPermanently(Long userId, String sessionId) {
        ragChatMessageMapper.deleteBySessionId(userId, sessionId);
        ragChatSessionMapper.deleteBySessionId(userId, sessionId);
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

    private KnowledgeChunk toKnowledgeChunk(RagChunk entity) {
        return new KnowledgeChunk(
                entity.getPostId(),
                entity.getPostTitle(),
                entity.getPostSlug(),
                entity.getChunkIndex(),
                entity.getContent(),
                entity.getPublishedAt(),
                readEmbedding(entity.getEmbeddingJson()),
                entity.getEmbeddingModel()
        );
    }

    private RagChunk toEntity(KnowledgeChunk chunk) {
        RagChunk entity = new RagChunk();
        entity.setPostId(chunk.postId());
        entity.setPostTitle(chunk.postTitle());
        entity.setPostSlug(chunk.postSlug());
        entity.setChunkIndex(chunk.chunkIndex());
        entity.setContent(chunk.content());
        entity.setPublishedAt(chunk.publishedAt());
        entity.setEmbeddingJson(writeEmbedding(chunk.embedding()));
        entity.setEmbeddingModel(chunk.embeddingModel());
        entity.setEmbeddingDimensions(chunk.embedding() == null ? null : chunk.embedding().length);
        return entity;
    }

    private ChatHistoryMessage toHistoryMessage(RagChatMessage entity) {
        return new ChatHistoryMessage(
                entity.getId(),
                entity.getSessionId(),
                entity.getRole(),
                entity.getContent(),
                entity.getAnswerMode(),
                readCitations(entity.getCitationsJson()),
                readSources(entity.getSourcesJson()),
                readVariants(entity.getVariantsJson()),
                entity.getCreatedAt(),
                entity.getFeedbackHelpful(),
                entity.getFeedbackNote(),
                entity.getFeedbackAt()
        );
    }

    private RagChatMessage toEntity(Long userId, ChatHistoryMessage message) {
        RagChatMessage entity = new RagChatMessage();
        entity.setId(message.id());
        entity.setSessionId(message.sessionId());
        entity.setUserId(userId);
        entity.setRole(message.role());
        entity.setContent(message.content());
        entity.setAnswerMode(message.mode());
        entity.setCitationsJson(writeCitations(message.citations()));
        entity.setSourcesJson(writeSources(message.sources()));
        entity.setVariantsJson(writeVariants(message.variants()));
        entity.setCreatedAt(message.createdAt());
        entity.setFeedbackHelpful(message.feedbackHelpful());
        entity.setFeedbackNote(message.feedbackNote());
        entity.setFeedbackAt(message.feedbackAt());
        return entity;
    }

    private double[] readEmbedding(String embeddingJson) {
        if (!StringUtils.hasText(embeddingJson)) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(embeddingJson, double[].class);
        } catch (Exception ex) {
            log.warn("Failed to parse chunk embedding: {}", ex.getMessage());
            return null;
        }
    }

    private ConversationSession toConversationSession(RagChatSession entity) {
        if (entity == null) {
            return null;
        }
        return new ConversationSession(
                entity.getSessionId(),
                entity.getTitle(),
                entity.getPreview(),
                entity.getMessageCount() == null ? 0 : entity.getMessageCount(),
                Boolean.TRUE.equals(entity.getManualTitle()),
                Boolean.TRUE.equals(entity.getDeleted()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private String writeEmbedding(double[] vector) {
        if (vector == null || vector.length == 0) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(vector);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize embedding", ex);
        }
    }

    private List<Integer> readCitations(String citationsJson) {
        if (!StringUtils.hasText(citationsJson)) {
            return List.of();
        }
        try {
            return OBJECT_MAPPER.readerForListOf(Integer.class).readValue(citationsJson);
        } catch (Exception ex) {
            log.warn("Failed to parse citations: {}", ex.getMessage());
            return List.of();
        }
    }

    private String writeCitations(List<Integer> citations) {
        if (citations == null || citations.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(citations);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize citations", ex);
        }
    }

    private List<com.hejulian.blog.dto.RagDtos.Source> readSources(String sourcesJson) {
        if (!StringUtils.hasText(sourcesJson)) {
            return List.of();
        }
        try {
            return OBJECT_MAPPER.readerForListOf(com.hejulian.blog.dto.RagDtos.Source.class).readValue(sourcesJson);
        } catch (Exception ex) {
            log.warn("Failed to parse sources: {}", ex.getMessage());
            return List.of();
        }
    }

    private String writeSources(List<com.hejulian.blog.dto.RagDtos.Source> sources) {
        if (sources == null || sources.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(sources);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize sources", ex);
        }
    }

    private List<com.hejulian.blog.dto.RagDtos.AnswerVariant> readVariants(String variantsJson) {
        if (!StringUtils.hasText(variantsJson)) {
            return List.of();
        }
        try {
            return OBJECT_MAPPER.readerForListOf(com.hejulian.blog.dto.RagDtos.AnswerVariant.class).readValue(variantsJson);
        } catch (Exception ex) {
            log.warn("Failed to parse variants: {}", ex.getMessage());
            return List.of();
        }
    }

    private String writeVariants(List<com.hejulian.blog.dto.RagDtos.AnswerVariant> variants) {
        if (variants == null || variants.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(variants);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize variants", ex);
        }
    }

    private void writeFingerprintCache(String fingerprint) {
        Cache cache = cacheManager.getCache(CacheNames.RAG_META);
        if (cache != null) {
            cache.put("index-fingerprint", fingerprint);
        }
    }

    private void clearCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }
}
