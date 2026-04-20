package com.hejulian.blog.rag.domain.model;

import com.hejulian.blog.dto.RagDtos;
import jakarta.annotation.Nullable;
import java.util.List;

public record AnswerContext(
        String sessionId,
        String question,
        IndexState indexState,
        List<ScoredChunk> rankedChunks,
        List<RagEvidence> evidences,
        List<RagDtos.Source> sources,
        String retrievalAnswer,
        List<String> followUpQuestions,
        List<RagDtos.ChatMessage> history,
        boolean llmPreferred,
        String searchMode,
        @Nullable RagDtos.AskResponse prebuiltResponse
) {
}
