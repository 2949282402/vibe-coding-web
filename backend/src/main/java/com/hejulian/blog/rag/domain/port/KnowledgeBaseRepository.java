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

    List<ChatHistoryMessage> loadConversationHistory(String sessionId);

    ChatHistoryMessage saveConversationMessage(ChatHistoryMessage message);

    ChatHistoryMessage updateConversationFeedback(String sessionId, Long messageId, Boolean helpful, String note);

    void deleteConversationMessagesFrom(String sessionId, Long fromMessageId);

    ConversationSession findConversationSession(String sessionId);

    List<ConversationSession> listConversationSessions(boolean includeDeleted, int limit);

    ConversationSession saveOrUpdateConversationSession(
            String sessionId,
            String generatedTitle,
            String preview,
            int messageCount
    );

    ConversationSession renameConversationSession(String sessionId, String title);

    void markConversationSessionDeleted(String sessionId, boolean deleted);

    void deleteConversationSessionPermanently(String sessionId);
}
