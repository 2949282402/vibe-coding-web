package com.hejulian.blog.rag.application;

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
    private static final String SEARCH_MODE_LOCAL_ONLY = "LOCAL_ONLY";
    private static final String SEARCH_MODE_LOCAL_AND_WEB = "LOCAL_AND_WEB";
    private static final String SOURCE_TYPE_BLOG = "blog";
    private static final String SOURCE_TYPE_WEB = "web";

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
                    context.prebuiltResponse().strictCitation()
            );
        }

        if (SEARCH_MODE_LOCAL_AND_WEB.equals(context.searchMode()) && chatModel.supportsWebSearch()) {
            WebSearchResolution resolution = resolveWithOfficialWebSearch(context);
            if (resolution != null) {
                List<Integer> citations = citationGuardService.extractCitationIndices(resolution.answer(), resolution.sources().size());
                List<RagDtos.ChatMessage> history = persistConversation(
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
                        resolution.strictCitation()
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
            if (citationGuardService.isStrictlyCited(generated, context.sources().size())) {
                answer = generated.trim();
                mode = "llm";
            }
        }

        List<Integer> citations = citationGuardService.extractCitationIndices(answer, context.sources().size());
        List<RagDtos.ChatMessage> history = persistConversation(
                context.sessionId(),
                context.question(),
                answer,
                mode,
                citations,
                context.sources()
        );
        return buildAnswerResponse(context, answer, mode, context.followUpQuestions(), context.sources(), history, true);
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

    public RagDtos.ChatMessage submitFeedback(RagDtos.FeedbackRequest request) {
        String sessionId = normalizeSessionId(request.sessionId());
        List<ChatHistoryMessage> history = knowledgeBaseRepository.loadConversationHistory(sessionId);
        ChatHistoryMessage target = history.stream()
                .filter(message -> Objects.equals(message.id(), request.messageId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation message not found"));

        if (!"assistant".equals(target.role())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only assistant messages can receive feedback");
        }

        ChatHistoryMessage updated = knowledgeBaseRepository.updateConversationFeedback(
                sessionId,
                target.id(),
                request.helpful(),
                normalizeFeedbackNote(request.note())
        );
        if (updated == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation message not found");
        }
        return toDtoMessage(updated);
    }

    private void streamQuestionInternal(RagDtos.AskRequest request, SseEmitter emitter) {
        AnswerContext context = buildAnswerContext(request);
        if (context.prebuiltResponse() != null) {
            List<RagDtos.ChatMessage> history = persistConversation(
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
                            context.prebuiltResponse().strictCitation()
                    ),
                    null
            );
            emitter.complete();
            return;
        }

        if (SEARCH_MODE_LOCAL_AND_WEB.equals(context.searchMode()) && chatModel.supportsWebSearch()) {
            sendEvent(
                    emitter,
                    "meta",
                    null,
                    buildAnswerResponse(context, "", "llm", context.followUpQuestions(), context.sources(), context.history(), false),
                    null
            );

            WebSearchResolution resolution = resolveWithOfficialWebSearch(context);
            if (resolution != null) {
                List<Integer> citations = citationGuardService.extractCitationIndices(resolution.answer(), resolution.sources().size());
                List<RagDtos.ChatMessage> history = persistConversation(
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
                                resolution.strictCitation()
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
                        true
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
            if (citationGuardService.isStrictlyCited(generated, context.sources().size())) {
                answer = generated.trim();
                mode = "llm";
            }
        }

        List<Integer> citations = citationGuardService.extractCitationIndices(answer, context.sources().size());
        List<RagDtos.ChatMessage> history = persistConversation(
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
                buildAnswerResponse(context, answer, mode, context.followUpQuestions(), context.sources(), history, true),
                null
        );
        emitter.complete();
    }

    private AnswerContext buildAnswerContext(RagDtos.AskRequest request) {
        String sessionId = normalizeSessionId(request.sessionId());
        String question = request.question().trim();
        String requestedSearchMode = normalizeSearchMode(request.searchMode());
        List<RagDtos.ChatMessage> history = toDtoHistory(
                limitHistory(knowledgeBaseRepository.loadConversationHistory(sessionId), MAX_RESPONSE_HISTORY_MESSAGES)
        );
        IndexState indexState = indexingService.ensureIndexReady();
        boolean webSearchRequested = isWebSearchRequested(requestedSearchMode);
        String searchMode = webSearchRequested ? SEARCH_MODE_LOCAL_AND_WEB : SEARCH_MODE_LOCAL_ONLY;

        if (indexState.chunkCount() == 0 && !webSearchRequested) {
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
                    searchMode
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

        int topK = normalizeTopK(request.topK());
        List<ScoredChunk> recalled = indexState.chunkCount() > 0 ? recallChunks(question, topK) : List.of();
        List<ScoredChunk> rankedChunks = recalled.isEmpty()
                ? List.of()
                : retrievalService.rerank(question, recalled, topK, rerankModel);

        List<RagEvidence> evidences = new ArrayList<>(buildBlogEvidence(rankedChunks));
        if (evidences.isEmpty() && !webSearchRequested) {
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
                    searchMode
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
            String sessionId,
            String question,
            String answer,
            String mode,
            List<Integer> citations,
            List<RagDtos.Source> sources
    ) {
        knowledgeBaseRepository.saveConversationMessage(new ChatHistoryMessage(
                null,
                sessionId,
                "user",
                question,
                null,
                List.of(),
                List.of(),
                LocalDateTime.now(),
                null,
                null,
                null
        ));
        knowledgeBaseRepository.saveConversationMessage(new ChatHistoryMessage(
                null,
                sessionId,
                "assistant",
                answer,
                mode,
                citations,
                sources,
                LocalDateTime.now(),
                null,
                null,
                null
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

    private void ensureSessionExists(String sessionId) {
        if (knowledgeBaseRepository.findConversationSession(sessionId) == null) {
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
                followUpQuestions,
                sources,
                history,
                strictCitation,
                context.searchMode()
        );
    }

    private String normalizeSearchMode(String searchMode) {
        if (SEARCH_MODE_LOCAL_AND_WEB.equalsIgnoreCase(searchMode)) {
            return SEARCH_MODE_LOCAL_AND_WEB;
        }
        return SEARCH_MODE_LOCAL_ONLY;
    }

    private boolean isWebSearchRequested(String searchMode) {
        return SEARCH_MODE_LOCAL_AND_WEB.equals(searchMode) && chatModel.supportsWebSearch();
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
