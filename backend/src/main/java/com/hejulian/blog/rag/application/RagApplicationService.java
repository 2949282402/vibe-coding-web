package com.hejulian.blog.rag.application;

import com.hejulian.blog.dto.RagDtos;
import com.hejulian.blog.rag.config.RagProperties;
import com.hejulian.blog.rag.domain.model.AnswerContext;
import com.hejulian.blog.rag.domain.model.ChatHistoryMessage;
import com.hejulian.blog.rag.domain.model.ConversationSession;
import com.hejulian.blog.rag.domain.model.IndexState;
import com.hejulian.blog.rag.domain.model.KnowledgeChunk;
import com.hejulian.blog.rag.domain.model.ScoredChunk;
import com.hejulian.blog.rag.domain.port.ChatModel;
import com.hejulian.blog.rag.domain.port.EmbeddingModel;
import com.hejulian.blog.rag.domain.port.KnowledgeBaseRepository;
import com.hejulian.blog.rag.domain.port.RerankModel;
import com.hejulian.blog.rag.domain.port.VectorStore;
import com.hejulian.blog.rag.domain.service.CitationGuardService;
import com.hejulian.blog.rag.domain.service.KnowledgeRetrievalService;
import com.hejulian.blog.rag.domain.service.KnowledgeTextProcessor;
import com.hejulian.blog.rag.domain.service.RagPromptService;
import jakarta.annotation.Nullable;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagApplicationService {

    private static final int MAX_PROMPT_HISTORY_MESSAGES = 8;
    private static final int MAX_RESPONSE_HISTORY_MESSAGES = 20;

    private final RagProperties ragProperties;
    private final RagIndexingApplicationService indexingService;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final EmbeddingModel embeddingModel;
    private final RerankModel rerankModel;
    private final VectorStore vectorStore;
    private final ChatModel chatModel;
    private final KnowledgeRetrievalService retrievalService;
    private final KnowledgeTextProcessor textProcessor;
    private final RagPromptService promptService;
    private final CitationGuardService citationGuardService;

    public RagDtos.AskResponse askQuestion(RagDtos.AskRequest request) {
        AnswerContext context = buildAnswerContext(request);
        if (context.prebuiltResponse() != null) {
            List<RagDtos.ChatMessage> history = persistConversation(
                    context.sessionId(),
                    context.question(),
                    context.prebuiltResponse().answer(),
                    context.prebuiltResponse().mode(),
                    List.of()
            );
            return buildAnswerResponse(
                    context,
                    context.prebuiltResponse().answer(),
                    context.prebuiltResponse().mode(),
                    history,
                    context.prebuiltResponse().strictCitation()
            );
        }

        String answer = context.retrievalAnswer();
        String mode = "retrieval";

        if (chatModel.isChatConfigured()) {
            String generated = chatModel.generate(
                    promptService.buildSystemPrompt(context.question()),
                    promptService.buildUserPrompt(context.question(), context.rankedChunks(), recentPromptHistory(context.history())),
                    0.2
            );
            if (citationGuardService.isStrictlyCited(generated, context.sources().size())) {
                answer = generated.trim();
                mode = "llm";
            }
        }

        List<Integer> citations = citationGuardService.extractCitationIndices(answer, context.sources().size());
        List<RagDtos.ChatMessage> history = persistConversation(context.sessionId(), context.question(), answer, mode, citations);
        return buildAnswerResponse(context, answer, mode, history, true);
    }

    public SseEmitter streamQuestion(RagDtos.AskRequest request) {
        SseEmitter emitter = new SseEmitter(ragProperties.getStreamTimeoutMillis());
        CompletableFuture.runAsync(() -> streamQuestionInternal(request, emitter))
                .exceptionally(ex -> {
                    log.warn("RAG stream failed: {}", ex.getMessage());
                    completeEmitterWithError(emitter, ex);
                    return null;
                });
        return emitter;
    }

    public int rebuildIndex() {
        return indexingService.rebuildIndex();
    }

    public RagDtos.HistoryResponse getHistory(String sessionId) {
        String normalizedSessionId = normalizeSessionId(sessionId);
        return new RagDtos.HistoryResponse(
                normalizedSessionId,
                toDtoHistory(limitHistory(knowledgeBaseRepository.loadConversationHistory(normalizedSessionId), MAX_RESPONSE_HISTORY_MESSAGES))
        );
    }

    public RagDtos.SessionListResponse getSessions(boolean includeDeleted) {
        List<RagDtos.SessionSummary> sessions = knowledgeBaseRepository.listConversationSessions(includeDeleted, 80).stream()
                .map(this::toSessionSummary)
                .toList();
        return new RagDtos.SessionListResponse(sessions);
    }

    public RagDtos.SessionSummary renameSession(String sessionId, String title) {
        String normalizedSessionId = normalizeSessionId(sessionId);
        ConversationSession renamed = knowledgeBaseRepository.renameConversationSession(normalizedSessionId, normalizeSessionTitle(title));
        if (renamed == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation session not found");
        }
        return toSessionSummary(renamed);
    }

    public void deleteSession(String sessionId) {
        String normalizedSessionId = normalizeSessionId(sessionId);
        ensureSessionExists(normalizedSessionId);
        knowledgeBaseRepository.markConversationSessionDeleted(normalizedSessionId, true);
    }

    public RagDtos.SessionSummary restoreSession(String sessionId) {
        String normalizedSessionId = normalizeSessionId(sessionId);
        ensureSessionExists(normalizedSessionId);
        knowledgeBaseRepository.markConversationSessionDeleted(normalizedSessionId, false);
        return toSessionSummary(knowledgeBaseRepository.findConversationSession(normalizedSessionId));
    }

    private void streamQuestionInternal(RagDtos.AskRequest request, SseEmitter emitter) {
        AnswerContext context = buildAnswerContext(request);
        if (context.prebuiltResponse() != null) {
            List<RagDtos.ChatMessage> history = persistConversation(
                    context.sessionId(),
                    context.question(),
                    context.prebuiltResponse().answer(),
                    context.prebuiltResponse().mode(),
                    List.of()
            );
            sendEvent(
                    emitter,
                    "done",
                    null,
                    buildAnswerResponse(
                            context,
                            context.prebuiltResponse().answer(),
                            context.prebuiltResponse().mode(),
                            history,
                            context.prebuiltResponse().strictCitation()
                    ),
                    null
            );
            emitter.complete();
            return;
        }

        sendEvent(
                emitter,
                "meta",
                null,
                buildAnswerResponse(context, "", chatModel.isChatConfigured() ? "llm" : "retrieval", context.history(), true),
                null
        );

        String answer = context.retrievalAnswer();
        String mode = "retrieval";

        if (chatModel.isChatConfigured()) {
            String generated = chatModel.streamGenerate(
                    promptService.buildSystemPrompt(context.question()),
                    promptService.buildUserPrompt(context.question(), context.rankedChunks(), recentPromptHistory(context.history())),
                    0.2,
                    delta -> sendEvent(emitter, "delta", delta, null, null)
            );
            if (citationGuardService.isStrictlyCited(generated, context.sources().size())) {
                answer = generated.trim();
                mode = "llm";
            }
        }

        List<Integer> citations = citationGuardService.extractCitationIndices(answer, context.sources().size());
        List<RagDtos.ChatMessage> history = persistConversation(context.sessionId(), context.question(), answer, mode, citations);
        sendEvent(emitter, "done", null, buildAnswerResponse(context, answer, mode, history, true), null);
        emitter.complete();
    }

    private AnswerContext buildAnswerContext(RagDtos.AskRequest request) {
        String sessionId = normalizeSessionId(request.sessionId());
        String question = request.question().trim();
        List<RagDtos.ChatMessage> history = toDtoHistory(
                limitHistory(knowledgeBaseRepository.loadConversationHistory(sessionId), MAX_RESPONSE_HISTORY_MESSAGES)
        );
        IndexState indexState = indexingService.ensureIndexReady();
        List<KnowledgeChunk> chunks = List.of();

        if (indexState.chunkCount() == 0) {
            RagDtos.AskResponse response = new RagDtos.AskResponse(
                    sessionId,
                    question,
                    promptService.localizedNoDataAnswer(question),
                    "retrieval",
                    chatModel.isChatConfigured(),
                    0,
                    0,
                    promptService.localizedFallbackSuggestions(question),
                    List.of(),
                    history,
                    true
            );
            return new AnswerContext(sessionId, question, indexState, List.of(), List.of(), response.answer(), response.followUpQuestions(), history, false, response);
        }

        int topK = normalizeTopK(request.topK());
        List<ScoredChunk> recalled = recallChunks(question, topK);
        if (recalled.isEmpty()) {
            RagDtos.AskResponse response = new RagDtos.AskResponse(
                    sessionId,
                    question,
                    promptService.localizedNoMatchAnswer(question),
                    "retrieval",
                    chatModel.isChatConfigured(),
                    indexState.postCount(),
                    indexState.chunkCount(),
                    promptService.localizedFallbackSuggestions(question),
                    List.of(),
                    history,
                    true
            );
            return new AnswerContext(sessionId, question, indexState, List.of(), List.of(), response.answer(), response.followUpQuestions(), history, false, response);
        }

        List<ScoredChunk> rankedChunks = retrievalService.rerank(question, recalled, topK, rerankModel);
        List<RagDtos.Source> sources = buildSources(rankedChunks);

        return new AnswerContext(
                sessionId,
                question,
                indexState,
                rankedChunks,
                sources,
                promptService.buildStrictRetrievalAnswer(question, rankedChunks),
                promptService.buildFollowUpQuestions(question, sources),
                history,
                chatModel.isChatConfigured(),
                null
        );
    }

    private List<ScoredChunk> recallChunks(String question, int topK) {
        int recallLimit = Math.max(topK, topK * ragProperties.getRecallMultiplier());

        if (vectorStore.isVectorStoreConfigured() && embeddingModel.isEmbeddingConfigured()) {
            double[] queryEmbedding = embeddingModel.embedQuery(question);
            List<ScoredChunk> vectorMatches = vectorStore.search(queryEmbedding, recallLimit, ragProperties.getMinScore());
            if (!vectorMatches.isEmpty()) {
                return vectorMatches;
            }
        }

        List<KnowledgeChunk> chunks = knowledgeBaseRepository.loadChunks();
        if (chunks.isEmpty()) {
            return List.of();
        }
        return retrievalService.recallLexical(
                question,
                chunks,
                topK,
                ragProperties.getMinScore(),
                ragProperties.getRecallMultiplier()
        );
    }

    private List<RagDtos.Source> buildSources(List<ScoredChunk> rankedChunks) {
        return rankedChunks.stream()
                .map(scored -> new RagDtos.Source(
                        scored.chunk().postId(),
                        scored.chunk().postTitle(),
                        scored.chunk().postSlug(),
                        textProcessor.summarizeExcerpt(scored.chunk().content()),
                        roundScore(scored.score()),
                        rankedChunks.indexOf(scored) + 1
                ))
                .toList();
    }

    private List<RagDtos.ChatMessage> persistConversation(String sessionId, String question, String answer, String mode, List<Integer> citations) {
        knowledgeBaseRepository.saveConversationMessage(new ChatHistoryMessage(
                null,
                sessionId,
                "user",
                question,
                null,
                List.of(),
                LocalDateTime.now()
        ));
        knowledgeBaseRepository.saveConversationMessage(new ChatHistoryMessage(
                null,
                sessionId,
                "assistant",
                answer,
                mode,
                citations,
                LocalDateTime.now()
        ));
        List<ChatHistoryMessage> savedHistory = knowledgeBaseRepository.loadConversationHistory(sessionId);
        knowledgeBaseRepository.saveOrUpdateConversationSession(
                sessionId,
                buildSessionTitle(question),
                buildSessionPreview(question),
                savedHistory.size()
        );
        return toDtoHistory(limitHistory(savedHistory, MAX_RESPONSE_HISTORY_MESSAGES));
    }

    private List<RagDtos.ChatMessage> toDtoHistory(List<ChatHistoryMessage> historyMessages) {
        return historyMessages.stream()
                .map(message -> new RagDtos.ChatMessage(
                        message.id(),
                        message.role(),
                        message.content(),
                        message.mode(),
                        message.citations(),
                        message.createdAt()
                ))
                .toList();
    }

    private List<ChatHistoryMessage> recentPromptHistory(List<RagDtos.ChatMessage> history) {
        return history.stream()
                .map(message -> new ChatHistoryMessage(
                        message.id(),
                        null,
                        message.role(),
                        message.content(),
                        message.mode(),
                        message.citations(),
                        message.createdAt()
                ))
                .skip(Math.max(0, history.size() - MAX_PROMPT_HISTORY_MESSAGES))
                .toList();
    }

    private <T> List<T> limitHistory(List<T> history, int maxSize) {
        if (history.size() <= maxSize) {
            return history;
        }
        return history.subList(history.size() - maxSize, history.size());
    }

    private String normalizeSessionId(String sessionId) {
        return StringUtils.hasText(sessionId) ? sessionId.trim() : UUID.randomUUID().toString();
    }

    private String normalizeSessionTitle(String value) {
        String normalized = collapseWhitespace(value);
        if (!StringUtils.hasText(normalized)) {
            return "New conversation";
        }
        return normalized.length() > 80 ? normalized.substring(0, 80).trim() : normalized;
    }

    private String buildSessionTitle(String question) {
        return normalizeSessionTitle(question);
    }

    private String buildSessionPreview(String question) {
        String normalized = collapseWhitespace(question);
        if (!StringUtils.hasText(normalized)) {
            return "";
        }
        return normalized.length() > 240 ? normalized.substring(0, 240).trim() : normalized;
    }

    private String collapseWhitespace(String input) {
        if (!StringUtils.hasText(input)) {
            return "";
        }
        return input.trim().replaceAll("\\s+", " ");
    }

    private int normalizeTopK(Integer requestedTopK) {
        if (requestedTopK == null) {
            return Math.min(Math.max(ragProperties.getDefaultTopK(), 1), 8);
        }
        return Math.min(Math.max(requestedTopK, 1), 8);
    }

    private RagDtos.SessionSummary toSessionSummary(ConversationSession session) {
        return new RagDtos.SessionSummary(
                session.sessionId(),
                session.title(),
                session.preview(),
                session.messageCount(),
                session.deleted(),
                session.createdAt(),
                session.updatedAt()
        );
    }

    private void ensureSessionExists(String sessionId) {
        if (knowledgeBaseRepository.findConversationSession(sessionId) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation session not found");
        }
    }

    private RagDtos.AskResponse buildAnswerResponse(
            AnswerContext context,
            String answer,
            String mode,
            List<RagDtos.ChatMessage> history,
            boolean strictCitation
    ) {
        return new RagDtos.AskResponse(
                context.sessionId(),
                context.question(),
                answer,
                mode,
                chatModel.isChatConfigured(),
                context.indexState().postCount(),
                context.indexState().chunkCount(),
                context.followUpQuestions(),
                context.sources(),
                history,
                strictCitation
        );
    }

    private double roundScore(double score) {
        return Math.round(score * 1000D) / 1000D;
    }

    private void sendEvent(
            SseEmitter emitter,
            String type,
            @Nullable String delta,
            @Nullable RagDtos.AskResponse response,
            @Nullable String message
    ) {
        try {
            emitter.send(SseEmitter.event()
                    .name(type)
                    .data(new RagDtos.StreamEvent(type, delta, response, message)));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to send SSE event", ex);
        }
    }

    private void completeEmitterWithError(SseEmitter emitter, Throwable ex) {
        try {
            sendEvent(emitter, "error", null, null, ex.getMessage());
            emitter.complete();
        } catch (Exception sendEx) {
            emitter.completeWithError(sendEx);
        }
    }
}
