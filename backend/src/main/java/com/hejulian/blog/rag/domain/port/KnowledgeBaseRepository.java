package com.hejulian.blog.rag.domain.port;

import com.hejulian.blog.rag.domain.model.KnowledgeChunk;
import com.hejulian.blog.rag.domain.model.PublishedPost;
import com.hejulian.blog.rag.domain.model.ChatHistoryMessage;
import com.hejulian.blog.rag.domain.model.ConversationSession;
import java.util.List;

public interface KnowledgeBaseRepository {

    List<PublishedPost> loadPublishedPosts();

    List<KnowledgeChunk> loadChunks();

    long countChunks();

    long countChunksWithoutEmbedding();

    String readFingerprint();

    void writeFingerprint(String fingerprint);

    void replaceChunks(List<KnowledgeChunk> chunks, String fingerprint);

    void replaceChunksByPostId(Long postId, List<KnowledgeChunk> chunks);

    void deleteChunksByPostId(Long postId);

    List<ChatHistoryMessage> loadConversationHistory(Long userId, String sessionId);

    ChatHistoryMessage saveConversationMessage(Long userId, ChatHistoryMessage message);

    ChatHistoryMessage updateConversationFeedback(Long userId, String sessionId, Long messageId, Boolean helpful, String note);

    ChatHistoryMessage updateConversationVariants(Long userId, String sessionId, Long messageId, List<com.hejulian.blog.dto.RagDtos.AnswerVariant> variants);

    void deleteConversationMessagesFrom(Long userId, String sessionId, Long fromMessageId);

    ConversationSession findConversationSession(Long userId, String sessionId);

    List<ConversationSession> listConversationSessions(Long userId, boolean includeDeleted, int limit);

    ConversationSession saveOrUpdateConversationSession(
            Long userId,
            String sessionId,
            String generatedTitle,
            String preview,
            int messageCount
    );

    ConversationSession renameConversationSession(Long userId, String sessionId, String title);

    void markConversationSessionDeleted(Long userId, String sessionId, boolean deleted);

    void deleteConversationSessionPermanently(Long userId, String sessionId);
}
