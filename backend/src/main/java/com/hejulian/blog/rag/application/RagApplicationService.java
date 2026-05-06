package com.hejulian.blog.rag.application;

import com.hejulian.blog.common.CacheNames;
import com.hejulian.blog.dto.RagDtos;
import com.hejulian.blog.rag.config.RagProperties;
import com.hejulian.blog.rag.domain.model.AnswerContext;
import com.hejulian.blog.rag.domain.model.ChatHistoryMessage;
import com.hejulian.blog.rag.domain.model.ConversationSession;
import com.hejulian.blog.rag.domain.model.IndexState;
import com.hejulian.blog.rag.domain.model.KnowledgeChunk;
import com.hejulian.blog.rag.domain.model.RagEvidence;
import com.hejulian.blog.rag.domain.model.ScoredChunk;
import com.hejulian.blog.rag.domain.model.WebSearchAnswer;
import com.hejulian.blog.rag.domain.model.WebSearchSource;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
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
    private static final String ANSWER_MODE_ASK = "ASK";
    private static final String SEARCH_MODE_LOCAL_ONLY = "LOCAL_ONLY";
    private static final String SEARCH_MODE_WEB_ONLY = "WEB_ONLY";
    private static final String SEARCH_MODE_LOCAL_AND_WEB = "LOCAL_AND_WEB";
    private static final String SOURCE_TYPE_BLOG = "blog";
    private static final String SOURCE_TYPE_WEB = "web";
    private static final String RESPONSE_SOURCE_TYPE_LOCAL = "local";
    private static final String RESPONSE_SOURCE_TYPE_WEB = "web";
    private static final String RESPONSE_SOURCE_TYPE_MIXED = "mixed";
    private static final String RESPONSE_SOURCE_TYPE_NONE = "none";
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
    private final CacheManager cacheManager;

    public RagDtos.AskResponse askQuestion(Long userId, RagDtos.AskRequest request) {
        long startedAt = System.nanoTime();
        if (isAskMode(request.answerMode())) {
            return askModelOnly(userId, request);
        }
        AnswerContext context = buildAnswerContext(userId, request);
        if (context.prebuiltResponse() != null) {
            List<RagDtos.ChatMessage> history = persistConversation(
                    userId,
                    context.sessionId(),
                    context.question(),
                    context.prebuiltResponse().answer(),
                    context.prebuiltResponse().mode(),
                    List.of(),
                    context.prebuiltResponse().sources()
            );
            return buildAnswerResponse(
                    context,
                    context.prebuiltResponse().answer(),
                    context.prebuiltResponse().mode(),
                    context.prebuiltResponse().followUpQuestions(),
                    context.prebuiltResponse().sources(),
                    history,
                    context.prebuiltResponse().strictCitation(),
                    false,
                    elapsedMillis(startedAt)
            );
        }

        if (isWebSearchMode(context.searchMode()) && chatModel.supportsWebSearch()) {
            WebSearchResolution resolution = resolveWithOfficialWebSearch(context);
            if (resolution != null) {
                List<Integer> citations = citationGuardService.extractCitationIndices(resolution.answer(), resolution.sources().size());
                List<RagDtos.ChatMessage> history = persistConversation(
                        userId,
                        context.sessionId(),
                        context.question(),
                        resolution.answer(),
                        resolution.mode(),
                        citations,
                        resolution.sources()
                );
                return buildAnswerResponse(
                        context,
                        resolution.answer(),
                        resolution.mode(),
                        resolution.followUpQuestions(),
                        resolution.sources(),
                        history,
                        resolution.strictCitation(),
                        true,
                        elapsedMillis(startedAt)
                );
            }
        }

        String answer = context.retrievalAnswer();
        String mode = "retrieval";

        if (chatModel.isChatConfigured()) {
            String generated = chatModel.generate(
                    promptService.buildSystemPrompt(context.question()),
                    promptService.buildUserPrompt(context.question(), context.evidences(), recentPromptHistory(context.history())),
                    0.2
            );
            if (shouldAcceptGeneratedAnswer(context, generated)) {
                answer = generated.trim();
                mode = "llm";
            }
        }

        List<Integer> citations = citationGuardService.extractCitationIndices(answer, context.sources().size());
        List<RagDtos.ChatMessage> history = persistConversation(
                userId,
                context.sessionId(),
                context.question(),
                answer,
                mode,
                citations,
                context.sources()
        );
        return buildAnswerResponse(context, answer, mode, context.followUpQuestions(), context.sources(), history, true, false, elapsedMillis(startedAt));
    }

    public SseEmitter streamQuestion(Long userId, RagDtos.AskRequest request) {
        return streamQuestion(userId, request, null);
    }

    public SseEmitter streamQuestion(
            Long userId,
            RagDtos.AskRequest request,
            @Nullable RagRuntimeContextHolder.RagRuntimeOptions runtimeOptions
    ) {
        SseEmitter emitter = new SseEmitter(ragProperties.getStreamTimeoutMillis());
                CompletableFuture.runAsync(() -> {
                    RagRuntimeContextHolder.set(runtimeOptions);
                    try {
                        streamQuestionInternal(userId, request, emitter);
                    } finally {
                        RagRuntimeContextHolder.clear();
                    }
                })
                .exceptionally(ex -> {
                    log.warn("RAG stream failed: {}", ex.getMessage());
                    completeEmitterWithError(emitter, ex);
                    return null;
                });
        return emitter;
    }

    public RagDtos.SearchResponse searchOnly(String rawQuestion) {
        String question = collapseWhitespace(rawQuestion);
        if (!StringUtils.hasText(question)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question must not be blank");
        }
        AnswerContext context = buildAnswerContext(
                UUID.randomUUID().toString(),
                question,
                SEARCH_MODE_LOCAL_ONLY,
                ragProperties.getDefaultTopK(),
                List.of()
        );
        return new RagDtos.SearchResponse(
                context.question(),
                context.retrievalAnswer(),
                context.followUpQuestions(),
                context.sources()
        );
    }

    public int rebuildIndex() {
        return indexingService.rebuildIndex();
    }

    @Cacheable(cacheNames = CacheNames.RAG_HISTORY, key = "#userId + ':' + (#sessionId == null ? '' : #sessionId.trim())")
    public RagDtos.HistoryResponse getHistory(Long userId, String sessionId) {
        String normalizedSessionId = normalizeSessionId(sessionId);
        return new RagDtos.HistoryResponse(
                normalizedSessionId,
                toDtoHistory(limitHistory(knowledgeBaseRepository.loadConversationHistory(userId, normalizedSessionId), MAX_RESPONSE_HISTORY_MESSAGES))
        );
    }

    @Cacheable(cacheNames = CacheNames.RAG_SESSION_LIST, key = "#userId + ':' + #includeDeleted")
    public RagDtos.SessionListResponse getSessions(Long userId, boolean includeDeleted) {
        List<RagDtos.SessionSummary> sessions = knowledgeBaseRepository.listConversationSessions(userId, includeDeleted, 80).stream()
                .map(this::toSessionSummary)
                .toList();
        return new RagDtos.SessionListResponse(sessions);
    }

    public RagDtos.SessionSummary renameSession(Long userId, String sessionId, String title) {
        String normalizedSessionId = normalizeSessionId(sessionId);
        ConversationSession renamed = knowledgeBaseRepository.renameConversationSession(userId, normalizedSessionId, normalizeSessionTitle(title));
        if (renamed == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation session not found");
        }
        evictSessionListCache(userId);
        return toSessionSummary(renamed);
    }

    public void deleteSession(Long userId, String sessionId) {
        String normalizedSessionId = normalizeSessionId(sessionId);
        ensureSessionExists(userId, normalizedSessionId);
        knowledgeBaseRepository.markConversationSessionDeleted(userId, normalizedSessionId, true);
        evictSessionListCache(userId);
    }

    public RagDtos.SessionSummary restoreSession(Long userId, String sessionId) {
        String normalizedSessionId = normalizeSessionId(sessionId);
        ensureSessionExists(userId, normalizedSessionId);
        knowledgeBaseRepository.markConversationSessionDeleted(userId, normalizedSessionId, false);
        evictSessionListCache(userId);
        return toSessionSummary(knowledgeBaseRepository.findConversationSession(userId, normalizedSessionId));
    }

    public void purgeSession(Long userId, String sessionId) {
        String normalizedSessionId = normalizeSessionId(sessionId);
        ConversationSession session = knowledgeBaseRepository.findConversationSession(userId, normalizedSessionId);
        if (session == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation session not found");
        }
        if (!session.deleted()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only deleted sessions can be permanently removed");
        }
        knowledgeBaseRepository.deleteConversationSessionPermanently(userId, normalizedSessionId);
        evictConversationCaches(userId, normalizedSessionId);
    }

    public RagDtos.ChatMessage submitFeedback(Long userId, RagDtos.FeedbackRequest request) {
        String sessionId = normalizeSessionId(request.sessionId());
        List<ChatHistoryMessage> history = knowledgeBaseRepository.loadConversationHistory(userId, sessionId);
        ChatHistoryMessage target = history.stream()
                .filter(message -> Objects.equals(message.id(), request.messageId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation message not found"));

        if (!"assistant".equals(target.role())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only assistant messages can receive feedback");
        }

        ChatHistoryMessage updated = knowledgeBaseRepository.updateConversationFeedback(
                userId,
                sessionId,
                target.id(),
                request.helpful(),
                normalizeFeedbackNote(request.note())
        );
        if (updated == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation message not found");
        }
        evictHistoryCache(userId, sessionId);
        return toDtoMessage(updated);
    }

    public RagDtos.AskResponse replayConversation(Long userId, RagDtos.ReplayRequest request) {
        long startedAt = System.nanoTime();
        String sessionId = normalizeSessionId(request.sessionId());
        ensureSessionExists(userId, sessionId);

        List<ChatHistoryMessage> fullHistory = knowledgeBaseRepository.loadConversationHistory(userId, sessionId);
        int targetIndex = findMessageIndex(fullHistory, request.messageId());
        ChatHistoryMessage target = fullHistory.get(targetIndex);
        ReplayTarget replayTarget = resolveReplayTarget(fullHistory, targetIndex, target, request.question());
        ChatHistoryMessage replayAssistant = resolveReplayAssistant(fullHistory, targetIndex, target);
        AnswerContext context = buildAnswerContext(
                sessionId,
                replayTarget.question(),
                request.searchMode(),
                request.topK(),
                replayTarget.baseHistory()
        );

        if (context.prebuiltResponse() != null) {
            List<RagDtos.ChatMessage> history = persistReplayVariant(
                    userId,
                    context.sessionId(),
                    replayAssistant,
                    context.question(),
                    context.prebuiltResponse().answer(),
                    context.prebuiltResponse().mode(),
                    List.of(),
                    context.prebuiltResponse().sources(),
                    fullHistory
            );
            return buildAnswerResponse(
                    context,
                    context.prebuiltResponse().answer(),
                    context.prebuiltResponse().mode(),
                    context.prebuiltResponse().followUpQuestions(),
                    context.prebuiltResponse().sources(),
                    history,
                    context.prebuiltResponse().strictCitation(),
                    false,
                    elapsedMillis(startedAt)
            );
        }

        if (isWebSearchMode(context.searchMode()) && chatModel.supportsWebSearch()) {
            WebSearchResolution resolution = resolveWithOfficialWebSearch(context);
            if (resolution != null) {
                List<Integer> citations = citationGuardService.extractCitationIndices(resolution.answer(), resolution.sources().size());
                List<RagDtos.ChatMessage> history = persistReplayVariant(
                        userId,
                        context.sessionId(),
                        replayAssistant,
                        context.question(),
                        resolution.answer(),
                        resolution.mode(),
                        citations,
                        resolution.sources(),
                        fullHistory
                );
                return buildAnswerResponse(
                        context,
                        resolution.answer(),
                        resolution.mode(),
                        resolution.followUpQuestions(),
                        resolution.sources(),
                        history,
                        resolution.strictCitation(),
                        true,
                        elapsedMillis(startedAt)
                );
            }
        }

        String answer = context.retrievalAnswer();
        String mode = "retrieval";

        if (chatModel.isChatConfigured()) {
            String generated = chatModel.generate(
                    promptService.buildSystemPrompt(context.question()),
                    promptService.buildUserPrompt(context.question(), context.evidences(), recentPromptHistory(context.history())),
                    0.2
            );
            if (shouldAcceptGeneratedAnswer(context, generated)) {
                answer = generated.trim();
                mode = "llm";
            }
        }

        List<Integer> citations = citationGuardService.extractCitationIndices(answer, context.sources().size());
        List<RagDtos.ChatMessage> history = persistReplayVariant(
                userId,
                context.sessionId(),
                replayAssistant,
                context.question(),
                answer,
                mode,
                citations,
                context.sources(),
                fullHistory
        );
        return buildAnswerResponse(context, answer, mode, context.followUpQuestions(), context.sources(), history, true, false, elapsedMillis(startedAt));
    }

    private void streamQuestionInternal(Long userId, RagDtos.AskRequest request, SseEmitter emitter) {
        long startedAt = System.nanoTime();
        if (isAskMode(request.answerMode())) {
            streamModelOnly(userId, request, emitter);
            return;
        }
        AnswerContext context = buildAnswerContext(userId, request);
        if (context.prebuiltResponse() != null) {
            List<RagDtos.ChatMessage> history = persistConversation(
                    userId,
                    context.sessionId(),
                    context.question(),
                    context.prebuiltResponse().answer(),
                    context.prebuiltResponse().mode(),
                    List.of(),
                    context.prebuiltResponse().sources()
            );
            sendEvent(
                    emitter,
                    "done",
                    null,
                    buildAnswerResponse(
                            context,
                            context.prebuiltResponse().answer(),
                            context.prebuiltResponse().mode(),
                            context.prebuiltResponse().followUpQuestions(),
                            context.prebuiltResponse().sources(),
                            history,
                            context.prebuiltResponse().strictCitation(),
                            false,
                            elapsedMillis(startedAt)
                    ),
                    null
            );
            emitter.complete();
            return;
        }

        if (isWebSearchMode(context.searchMode()) && chatModel.supportsWebSearch()) {
            sendEvent(
                    emitter,
                    "meta",
                    null,
                    buildAnswerResponse(context, "", "llm", context.followUpQuestions(), context.sources(), context.history(), false, false, 0L),
                    null
            );

            WebSearchResolution resolution = resolveWithOfficialWebSearch(context);
            if (resolution != null) {
                List<Integer> citations = citationGuardService.extractCitationIndices(resolution.answer(), resolution.sources().size());
                List<RagDtos.ChatMessage> history = persistConversation(
                        userId,
                        context.sessionId(),
                        context.question(),
                        resolution.answer(),
                        resolution.mode(),
                        citations,
                        resolution.sources()
                );
                sendEvent(
                        emitter,
                        "done",
                        null,
                        buildAnswerResponse(
                                context,
                                resolution.answer(),
                                resolution.mode(),
                                resolution.followUpQuestions(),
                                resolution.sources(),
                                history,
                                resolution.strictCitation(),
                                true,
                                elapsedMillis(startedAt)
                        ),
                        null
                );
                emitter.complete();
                return;
            }
        }

        sendEvent(
                emitter,
                "meta",
                null,
                buildAnswerResponse(
                        context,
                        "",
                        chatModel.isChatConfigured() ? "llm" : "retrieval",
                        context.followUpQuestions(),
                        context.sources(),
                        context.history(),
                        true,
                        false,
                        0L
                ),
                null
        );

        String answer = context.retrievalAnswer();
        String mode = "retrieval";

        if (chatModel.isChatConfigured()) {
            String generated = chatModel.streamGenerate(
                    promptService.buildSystemPrompt(context.question()),
                    promptService.buildUserPrompt(context.question(), context.evidences(), recentPromptHistory(context.history())),
                    0.2,
                    delta -> sendEvent(emitter, "delta", delta, null, null)
            );
            if (shouldAcceptGeneratedAnswer(context, generated)) {
                answer = generated.trim();
                mode = "llm";
            }
        }

        List<Integer> citations = citationGuardService.extractCitationIndices(answer, context.sources().size());
        List<RagDtos.ChatMessage> history = persistConversation(
                userId,
                context.sessionId(),
                context.question(),
                answer,
                mode,
                citations,
                context.sources()
        );
        sendEvent(
                emitter,
                "done",
                null,
                buildAnswerResponse(context, answer, mode, context.followUpQuestions(), context.sources(), history, true, false, elapsedMillis(startedAt)),
                null
        );
        emitter.complete();
    }

    private AnswerContext buildAnswerContext(Long userId, RagDtos.AskRequest request) {
        String sessionId = normalizeSessionId(request.sessionId());
        List<ChatHistoryMessage> baseHistory = limitHistory(
                knowledgeBaseRepository.loadConversationHistory(userId, sessionId),
                MAX_RESPONSE_HISTORY_MESSAGES
        );
        return buildAnswerContext(sessionId, request.question(), request.searchMode(), request.topK(), baseHistory);
    }

    private RagDtos.AskResponse askModelOnly(Long userId, RagDtos.AskRequest request) {
        long startedAt = System.nanoTime();
        String sessionId = normalizeSessionId(request.sessionId());
        String question = request.question().trim();
        IndexState indexState = indexingService.ensureIndexReady();
        List<ChatHistoryMessage> baseHistory = limitHistory(
                knowledgeBaseRepository.loadConversationHistory(userId, sessionId),
                MAX_RESPONSE_HISTORY_MESSAGES
        );
        String answer = generateModelOnlyAnswer(question, baseHistory, null);
        List<RagDtos.ChatMessage> history = persistConversation(
                userId,
                sessionId,
                question,
                answer,
                "ask",
                List.of(),
                List.of()
        );
        return buildModelOnlyResponse(
                sessionId,
                question,
                answer,
                normalizeSearchMode(request.searchMode()),
                indexState,
                history,
                elapsedMillis(startedAt)
        );
    }

    private void streamModelOnly(Long userId, RagDtos.AskRequest request, SseEmitter emitter) {
        long startedAt = System.nanoTime();
        String sessionId = normalizeSessionId(request.sessionId());
        String question = request.question().trim();
        String searchMode = normalizeSearchMode(request.searchMode());
        IndexState indexState = indexingService.ensureIndexReady();
        List<ChatHistoryMessage> baseHistory = limitHistory(
                knowledgeBaseRepository.loadConversationHistory(userId, sessionId),
                MAX_RESPONSE_HISTORY_MESSAGES
        );
        List<RagDtos.ChatMessage> history = toDtoHistory(baseHistory);
        sendEvent(
                emitter,
                "meta",
                null,
                buildModelOnlyResponse(sessionId, question, "", searchMode, indexState, history, 0L),
                null
        );

        String answer = generateModelOnlyAnswer(
                question,
                baseHistory,
                delta -> sendEvent(emitter, "delta", delta, null, null)
        );
        List<RagDtos.ChatMessage> persistedHistory = persistConversation(
                userId,
                sessionId,
                question,
                answer,
                "ask",
                List.of(),
                List.of()
        );
        sendEvent(
                emitter,
                "done",
                null,
                buildModelOnlyResponse(sessionId, question, answer, searchMode, indexState, persistedHistory, elapsedMillis(startedAt)),
                null
        );
        emitter.complete();
    }

    private AnswerContext buildAnswerContext(
            String sessionId,
            String rawQuestion,
            String rawSearchMode,
            Integer requestedTopK,
            List<ChatHistoryMessage> baseHistory
    ) {
        String question = rawQuestion.trim();
        String requestedSearchMode = normalizeSearchMode(rawSearchMode);
        List<RagDtos.ChatMessage> history = toDtoHistory(limitHistory(baseHistory, MAX_RESPONSE_HISTORY_MESSAGES));
        IndexState indexState = indexingService.ensureIndexReady();
        boolean webSearchRequested = isWebSearchRequested(requestedSearchMode);
        boolean localSearchRequested = !SEARCH_MODE_WEB_ONLY.equals(requestedSearchMode);
        String searchMode = webSearchRequested ? requestedSearchMode : SEARCH_MODE_LOCAL_ONLY;

        if (indexState.chunkCount() == 0 && localSearchRequested && !webSearchRequested) {
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
                    true,
                    searchMode,
                    RESPONSE_SOURCE_TYPE_NONE,
                    0,
                    false,
                    0L,
                    "low",
                    null
            );
            return new AnswerContext(
                    sessionId,
                    question,
                    indexState,
                    List.of(),
                    List.of(),
                    List.of(),
                    response.answer(),
                    response.followUpQuestions(),
                    history,
                    false,
                    searchMode,
                    response
            );
        }

        int topK = normalizeTopK(requestedTopK);
        List<ScoredChunk> recalled = localSearchRequested && indexState.chunkCount() > 0 ? recallChunks(question, topK) : List.of();
        List<ScoredChunk> rankedChunks = recalled.isEmpty()
                ? List.of()
                : retrievalService.rerank(question, recalled, topK, rerankModel);

        List<RagEvidence> evidences = new ArrayList<>(buildBlogEvidence(rankedChunks));
        if (evidences.isEmpty() && localSearchRequested && !webSearchRequested) {
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
                    true,
                    searchMode,
                    RESPONSE_SOURCE_TYPE_NONE,
                    0,
                    false,
                    0L,
                    "low",
                    null
            );
            return new AnswerContext(
                    sessionId,
                    question,
                    indexState,
                    List.of(),
                    List.of(),
                    List.of(),
                    response.answer(),
                    response.followUpQuestions(),
                    history,
                    false,
                    searchMode,
                    response
            );
        }

        List<RagDtos.Source> sources = buildSources(evidences);
        String retrievalAnswer = evidences.isEmpty()
                ? promptService.localizedNoMatchAnswer(question)
                : promptService.buildStrictRetrievalAnswer(question, evidences);
        List<String> followUpQuestions = sources.isEmpty()
                ? promptService.localizedFallbackSuggestions(question)
                : promptService.buildFollowUpQuestions(question, sources);

        return new AnswerContext(
                sessionId,
                question,
                indexState,
                rankedChunks,
                evidences,
                sources,
                retrievalAnswer,
                followUpQuestions,
                history,
                chatModel.isChatConfigured(),
                searchMode,
                null
        );
    }

    private int findMessageIndex(List<ChatHistoryMessage> history, Long messageId) {
        for (int index = 0; index < history.size(); index += 1) {
            if (Objects.equals(history.get(index).id(), messageId)) {
                return index;
            }
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation message not found");
    }

    private ReplayTarget resolveReplayTarget(
            List<ChatHistoryMessage> fullHistory,
            int targetIndex,
            ChatHistoryMessage target,
            String editedQuestion
    ) {
        if ("user".equals(target.role())) {
            String question = collapseWhitespace(editedQuestion);
            if (!StringUtils.hasText(question)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Edited question must not be blank");
            }
            return new ReplayTarget(
                    target.id(),
                    question,
                    new ArrayList<>(fullHistory.subList(0, targetIndex))
            );
        }

        if (!"assistant".equals(target.role())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only user or assistant messages can be replayed");
        }

        for (int index = targetIndex - 1; index >= 0; index -= 1) {
            ChatHistoryMessage candidate = fullHistory.get(index);
            if ("user".equals(candidate.role())) {
                return new ReplayTarget(
                        candidate.id(),
                        candidate.content(),
                        new ArrayList<>(fullHistory.subList(0, index))
                );
            }
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No user message found before the selected assistant response");
    }

    private ChatHistoryMessage resolveReplayAssistant(List<ChatHistoryMessage> fullHistory, int targetIndex, ChatHistoryMessage target) {
        if ("assistant".equals(target.role())) {
            return target;
        }
        for (int index = targetIndex + 1; index < fullHistory.size(); index += 1) {
            ChatHistoryMessage candidate = fullHistory.get(index);
            if ("assistant".equals(candidate.role())) {
                return candidate;
            }
            if ("user".equals(candidate.role())) {
                break;
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No assistant response found after the selected user message");
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

    private List<RagEvidence> buildBlogEvidence(List<ScoredChunk> rankedChunks) {
        return rankedChunks.stream()
                .map(scored -> new RagEvidence(
                        SOURCE_TYPE_BLOG,
                        scored.chunk().postTitle(),
                        scored.chunk().postSlug(),
                        scored.chunk().content(),
                        roundScore(scored.score()),
                        rankedChunks.indexOf(scored) + 1,
                        scored.chunk().postId(),
                        scored.chunk().postSlug(),
                        null,
                        null
                ))
                .toList();
    }

    private List<RagDtos.Source> buildSources(List<RagEvidence> evidences) {
        return evidences.stream()
                .map(evidence -> new RagDtos.Source(
                        evidence.postId(),
                        evidence.title(),
                        evidence.slug(),
                        textProcessor.summarizeExcerpt(evidence.content()),
                        roundScore(evidence.score()),
                        evidence.citationIndex(),
                        evidence.sourceType(),
                        evidence.url(),
                        evidence.domain()
                ))
                .toList();
    }

    private List<RagDtos.ChatMessage> persistConversation(
            Long userId,
            String sessionId,
            String question,
            String answer,
            String mode,
            List<Integer> citations,
            List<RagDtos.Source> sources
    ) {
        knowledgeBaseRepository.saveConversationMessage(userId, new ChatHistoryMessage(
                null,
                sessionId,
                "user",
                question,
                null,
                List.of(),
                List.of(),
                List.of(),
                LocalDateTime.now(),
                null,
                null,
                null
        ));
        knowledgeBaseRepository.saveConversationMessage(userId, new ChatHistoryMessage(
                null,
                sessionId,
                "assistant",
                answer,
                mode,
                citations,
                sources,
                List.of(),
                LocalDateTime.now(),
                null,
                null,
                null
        ));
        List<ChatHistoryMessage> savedHistory = knowledgeBaseRepository.loadConversationHistory(userId, sessionId);
        knowledgeBaseRepository.saveOrUpdateConversationSession(
                userId,
                sessionId,
                buildSessionTitle(question),
                buildSessionPreview(question),
                savedHistory.size()
        );
        evictConversationCaches(userId, sessionId);
        return toDtoHistory(limitHistory(savedHistory, MAX_RESPONSE_HISTORY_MESSAGES));
    }

    private List<RagDtos.ChatMessage> persistReplayVariant(
            Long userId,
            String sessionId,
            ChatHistoryMessage replayAssistant,
            String question,
            String answer,
            String mode,
            List<Integer> citations,
            List<RagDtos.Source> sources,
            List<ChatHistoryMessage> fullHistory
    ) {
        Long deleteFromMessageId = resolveDeleteStartMessageId(fullHistory, replayAssistant.id());
        if (deleteFromMessageId != null) {
            knowledgeBaseRepository.deleteConversationMessagesFrom(userId, sessionId, deleteFromMessageId);
        }

        List<RagDtos.AnswerVariant> variants = new ArrayList<>(replayAssistant.variants() == null ? List.of() : replayAssistant.variants());
        variants.add(new RagDtos.AnswerVariant(
                question,
                answer,
                mode,
                citations,
                sources,
                LocalDateTime.now()
        ));
        ChatHistoryMessage updated = knowledgeBaseRepository.updateConversationVariants(userId, sessionId, replayAssistant.id(), variants);
        if (updated == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation message not found");
        }

        List<ChatHistoryMessage> savedHistory = knowledgeBaseRepository.loadConversationHistory(userId, sessionId);
        knowledgeBaseRepository.saveOrUpdateConversationSession(
                userId,
                sessionId,
                buildSessionTitle(question),
                buildSessionPreview(question),
                savedHistory.size()
        );
        evictConversationCaches(userId, sessionId);
        return toDtoHistory(limitHistory(savedHistory, MAX_RESPONSE_HISTORY_MESSAGES));
    }

    private Long resolveDeleteStartMessageId(List<ChatHistoryMessage> fullHistory, Long assistantMessageId) {
        if (assistantMessageId == null) {
            return null;
        }
        boolean seenAssistant = false;
        for (ChatHistoryMessage message : fullHistory) {
            if (!seenAssistant) {
                seenAssistant = Objects.equals(message.id(), assistantMessageId);
                continue;
            }
            return message.id();
        }
        return null;
    }

    private void evictConversationCaches(Long userId, String sessionId) {
        evictHistoryCache(userId, sessionId);
        evictSessionListCache(userId);
    }

    private void evictHistoryCache(Long userId, String sessionId) {
        Cache cache = cacheManager.getCache(CacheNames.RAG_HISTORY);
        if (cache != null && StringUtils.hasText(sessionId)) {
            cache.evict(userId + ":" + sessionId.trim());
        }
    }

    private void evictSessionListCache(Long userId) {
        Cache cache = cacheManager.getCache(CacheNames.RAG_SESSION_LIST);
        if (cache != null) {
            cache.evict(userId + ":" + Boolean.TRUE);
            cache.evict(userId + ":" + Boolean.FALSE);
        }
    }

    private List<RagDtos.ChatMessage> toDtoHistory(List<ChatHistoryMessage> historyMessages) {
        return historyMessages.stream()
                .map(this::toDtoMessage)
                .toList();
    }

    private RagDtos.ChatMessage toDtoMessage(ChatHistoryMessage message) {
        return new RagDtos.ChatMessage(
                message.id(),
                message.role(),
                message.content(),
                message.mode(),
                message.citations(),
                message.sources(),
                message.variants(),
                message.createdAt(),
                message.feedbackHelpful(),
                message.feedbackNote(),
                message.feedbackAt()
        );
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
                        message.sources(),
                        message.variants(),
                        message.createdAt(),
                        message.feedbackHelpful(),
                        message.feedbackNote(),
                        message.feedbackAt()
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

    private String normalizeFeedbackNote(String note) {
        String normalized = collapseWhitespace(note);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        return normalized.length() > 1000 ? normalized.substring(0, 1000).trim() : normalized;
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

    private void ensureSessionExists(Long userId, String sessionId) {
        if (knowledgeBaseRepository.findConversationSession(userId, sessionId) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation session not found");
        }
    }

    private RagDtos.AskResponse buildAnswerResponse(
            AnswerContext context,
            String answer,
            String mode,
            List<String> followUpQuestions,
            List<RagDtos.Source> sources,
            List<RagDtos.ChatMessage> history,
            boolean strictCitation,
            boolean usedWebSearch,
            long latencyMs
    ) {
        AnswerMetadata metadata = buildAnswerMetadata(context, sources, strictCitation, usedWebSearch);
        return new RagDtos.AskResponse(
                context.sessionId(),
                context.question(),
                answer,
                mode,
                chatModel.isChatConfigured(),
                context.indexState().postCount(),
                context.indexState().chunkCount(),
                followUpQuestions,
                sources,
                history,
                strictCitation,
                context.searchMode(),
                metadata.sourceType(),
                metadata.retrievalHitCount(),
                usedWebSearch,
                latencyMs,
                metadata.confidenceLevel(),
                metadata.knowledgeUpdatedAt()
        );
    }

    private RagDtos.AskResponse buildModelOnlyResponse(
            String sessionId,
            String question,
            String answer,
            String searchMode,
            IndexState indexState,
            List<RagDtos.ChatMessage> history,
            long latencyMs
    ) {
        return new RagDtos.AskResponse(
                sessionId,
                question,
                answer,
                "ask",
                chatModel.isChatConfigured(),
                indexState.postCount(),
                indexState.chunkCount(),
                List.of(),
                List.of(),
                history,
                false,
                searchMode,
                RESPONSE_SOURCE_TYPE_NONE,
                0,
                false,
                latencyMs,
                "low",
                null
        );
    }

    private AnswerMetadata buildAnswerMetadata(
            AnswerContext context,
            List<RagDtos.Source> sources,
            boolean strictCitation,
            boolean usedWebSearch
    ) {
        int retrievalHitCount = countLocalSources(sources);
        boolean hasLocalSources = retrievalHitCount > 0;
        boolean hasWebSources = hasWebSources(sources);
        String sourceType = resolveSourceType(hasLocalSources, hasWebSources);
        String confidenceLevel = resolveConfidenceLevel(sourceType, retrievalHitCount, strictCitation, usedWebSearch);
        return new AnswerMetadata(
                sourceType,
                retrievalHitCount,
                confidenceLevel,
                resolveKnowledgeUpdatedAt(context)
        );
    }

    private int countLocalSources(List<RagDtos.Source> sources) {
        return (int) sources.stream()
                .filter(source -> SOURCE_TYPE_BLOG.equals(source.sourceType()))
                .count();
    }

    private boolean hasWebSources(List<RagDtos.Source> sources) {
        return sources.stream().anyMatch(source -> SOURCE_TYPE_WEB.equals(source.sourceType()));
    }

    private String resolveSourceType(boolean hasLocalSources, boolean hasWebSources) {
        if (hasLocalSources && hasWebSources) {
            return RESPONSE_SOURCE_TYPE_MIXED;
        }
        if (hasLocalSources) {
            return RESPONSE_SOURCE_TYPE_LOCAL;
        }
        if (hasWebSources) {
            return RESPONSE_SOURCE_TYPE_WEB;
        }
        return RESPONSE_SOURCE_TYPE_NONE;
    }

    private String resolveConfidenceLevel(
            String sourceType,
            int retrievalHitCount,
            boolean strictCitation,
            boolean usedWebSearch
    ) {
        if (RESPONSE_SOURCE_TYPE_NONE.equals(sourceType)) {
            return "low";
        }
        if (RESPONSE_SOURCE_TYPE_WEB.equals(sourceType)) {
            return "low";
        }
        if (RESPONSE_SOURCE_TYPE_MIXED.equals(sourceType)) {
            return strictCitation && retrievalHitCount >= 2 ? "medium" : "low";
        }
        if (strictCitation && retrievalHitCount >= 2 && !usedWebSearch) {
            return "high";
        }
        if (retrievalHitCount >= 1) {
            return "medium";
        }
        return "low";
    }

    private LocalDateTime resolveKnowledgeUpdatedAt(AnswerContext context) {
        return context.rankedChunks().stream()
                .map(ScoredChunk::chunk)
                .map(KnowledgeChunk::publishedAt)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private long elapsedMillis(long startedAt) {
        return Math.max(0L, (System.nanoTime() - startedAt) / 1_000_000L);
    }

    private record AnswerMetadata(
            String sourceType,
            int retrievalHitCount,
            String confidenceLevel,
            @Nullable LocalDateTime knowledgeUpdatedAt
    ) {
    }

    private String generateModelOnlyAnswer(
            String question,
            List<ChatHistoryMessage> history,
            @Nullable Consumer<String> deltaConsumer
    ) {
        if (!chatModel.isChatConfigured()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chat model is not configured");
        }
        String systemPrompt = """
                You are a helpful conversational assistant.
                Answer the user directly without using retrieval, citations, hidden tool traces, or blog-source assumptions.
                Use the same language as the user unless they ask otherwise.
                """;
        String userPrompt = buildModelOnlyUserPrompt(question, history);
        String answer = deltaConsumer != null
                ? chatModel.streamGenerate(systemPrompt, userPrompt, 0.3D, deltaConsumer)
                : chatModel.generate(systemPrompt, userPrompt, 0.3D);
        if (!StringUtils.hasText(answer)) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Chat model returned an empty answer");
        }
        return answer.trim();
    }

    private String buildModelOnlyUserPrompt(String question, List<ChatHistoryMessage> history) {
        List<ChatHistoryMessage> promptHistory = limitHistory(history, MAX_PROMPT_HISTORY_MESSAGES);
        String historyText = promptHistory.isEmpty()
                ? "No previous conversation."
                : promptHistory.stream()
                .map(message -> "%s: %s".formatted(message.role(), message.content()))
                .toList()
                .toString();
        return """
                Recent conversation:
                %s

                User question:
                %s
                """.formatted(historyText, question);
    }

    private String normalizeSearchMode(String searchMode) {
        if (SEARCH_MODE_WEB_ONLY.equalsIgnoreCase(searchMode)) {
            return SEARCH_MODE_WEB_ONLY;
        }
        if (SEARCH_MODE_LOCAL_AND_WEB.equalsIgnoreCase(searchMode)) {
            return SEARCH_MODE_LOCAL_AND_WEB;
        }
        return SEARCH_MODE_LOCAL_ONLY;
    }

    private boolean isAskMode(String answerMode) {
        return ANSWER_MODE_ASK.equalsIgnoreCase(answerMode);
    }

    private boolean shouldAcceptGeneratedAnswer(AnswerContext context, @Nullable String generated) {
        if (!StringUtils.hasText(generated)) {
            return false;
        }
        String normalized = generated.trim();
        if (citationGuardService.isStrictlyCited(normalized, context.sources().size())) {
            return true;
        }
        return normalized.equals(promptService.localizedNoMatchAnswer(context.question()))
                || normalized.equals(promptService.localizedNoDataAnswer(context.question()));
    }

    private boolean isWebSearchRequested(String searchMode) {
        return isWebSearchMode(searchMode) && chatModel.supportsWebSearch();
    }

    private boolean isWebSearchMode(String searchMode) {
        return SEARCH_MODE_LOCAL_AND_WEB.equals(searchMode) || SEARCH_MODE_WEB_ONLY.equals(searchMode);
    }

    @Nullable
    private WebSearchResolution resolveWithOfficialWebSearch(AnswerContext context) {
        WebSearchAnswer webSearchAnswer = chatModel.generateWithWebSearch(
                promptService.buildWebSearchSystemPrompt(context.question()),
                promptService.buildWebSearchUserPrompt(context.question(), context.evidences(), recentPromptHistory(context.history())),
                0.2
        );
        if (webSearchAnswer == null) {
            return null;
        }

        List<RagDtos.Source> mergedSources = mergeSources(context.sources(), webSearchAnswer.sources());
        List<String> followUpQuestions = promptService.buildFollowUpQuestions(context.question(), mergedSources);
        String answer = normalizeWebSearchAnswer(webSearchAnswer.answer(), context.sources().size());
        if (!StringUtils.hasText(answer)) {
            answer = context.retrievalAnswer();
        }
        return new WebSearchResolution(answer, mergedSources, followUpQuestions, "llm", false);
    }

    private List<RagDtos.Source> mergeSources(List<RagDtos.Source> localSources, List<WebSearchSource> webSources) {
        List<RagDtos.Source> merged = new ArrayList<>(localSources);
        int localCount = localSources.size();
        for (WebSearchSource source : webSources) {
            String domain = extractDomain(source.url());
            merged.add(new RagDtos.Source(
                    null,
                    StringUtils.hasText(source.title()) ? source.title() : defaultWebSourceTitle(source.url()),
                    null,
                    StringUtils.hasText(source.siteName()) ? source.siteName() : domain,
                    roundScore(Math.max(0.3D, 0.9D - (source.index() * 0.06D))),
                    localCount + source.index(),
                    SOURCE_TYPE_WEB,
                    source.url(),
                    domain
            ));
        }
        return List.copyOf(merged);
    }

    private String normalizeWebSearchAnswer(String answer, int localSourceCount) {
        if (!StringUtils.hasText(answer)) {
            return "";
        }

        String normalized = answer.trim();
        for (int index = 1; index <= 12; index++) {
            normalized = normalized.replace("[ref_" + index + "]", "[" + (localSourceCount + index) + "]");
        }
        return normalized;
    }

    private String extractDomain(String url) {
        if (!StringUtils.hasText(url)) {
            return "";
        }

        String normalized = url.replaceFirst("^https?://", "");
        int slashIndex = normalized.indexOf('/');
        String host = slashIndex >= 0 ? normalized.substring(0, slashIndex) : normalized;
        return host.startsWith("www.") ? host.substring(4) : host;
    }

    private String defaultWebSourceTitle(String url) {
        String domain = extractDomain(url);
        return StringUtils.hasText(domain) ? domain : "Web result";
    }

    private record WebSearchResolution(
            String answer,
            List<RagDtos.Source> sources,
            List<String> followUpQuestions,
            String mode,
            boolean strictCitation
    ) {
    }

    private record ReplayTarget(
            Long fromMessageId,
            String question,
            List<ChatHistoryMessage> baseHistory
    ) {
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
