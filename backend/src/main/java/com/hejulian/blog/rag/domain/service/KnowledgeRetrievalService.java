package com.hejulian.blog.rag.domain.service;

import com.hejulian.blog.rag.domain.model.KnowledgeChunk;
import com.hejulian.blog.rag.domain.model.RerankResult;
import com.hejulian.blog.rag.domain.model.ScoredChunk;
import com.hejulian.blog.rag.domain.port.EmbeddingModel;
import com.hejulian.blog.rag.domain.port.RerankModel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class KnowledgeRetrievalService {

    private static final Pattern LATIN_TOKEN_PATTERN = Pattern.compile("[a-z0-9]{2,}");
    private static final Pattern CJK_SEGMENT_PATTERN = Pattern.compile("[\\u4e00-\\u9fff]+");

    private final KnowledgeTextProcessor textProcessor;

    public List<ScoredChunk> recall(String question, List<KnowledgeChunk> chunks, int topK, EmbeddingModel embeddingModel,
                                    double minScore, int recallMultiplier) {
        if (embeddingModel.isEmbeddingConfigured()) {
            List<ScoredChunk> byEmbedding = recallByEmbedding(question, chunks, topK, embeddingModel, minScore, recallMultiplier);
            if (!byEmbedding.isEmpty()) {
                return byEmbedding;
            }
        }
        return recallByLexical(question, chunks, topK, minScore, recallMultiplier);
    }

    public List<ScoredChunk> recallLexical(String question, List<KnowledgeChunk> chunks, int topK,
                                           double minScore, int recallMultiplier) {
        return recallByLexical(question, chunks, topK, minScore, recallMultiplier);
    }

    public List<ScoredChunk> rerank(String question, List<ScoredChunk> recalled, int topK, RerankModel rerankModel) {
        if (!rerankModel.isRerankConfigured() || recalled.size() <= 1) {
            return recalled.stream().limit(topK).toList();
        }

        List<String> documents = recalled.stream()
                .map(scored -> buildEmbeddingText(scored.chunk()))
                .toList();
        List<RerankResult> reranked = rerankModel.rerank(question, documents, Math.min(topK, documents.size()));
        if (reranked.isEmpty()) {
            return recalled.stream().limit(topK).toList();
        }

        List<ScoredChunk> finalChunks = new ArrayList<>();
        for (RerankResult result : reranked) {
            if (result.index() >= 0 && result.index() < recalled.size()) {
                finalChunks.add(new ScoredChunk(recalled.get(result.index()).chunk(), result.relevanceScore()));
            }
        }
        return finalChunks.isEmpty() ? recalled.stream().limit(topK).toList() : finalChunks;
    }

    public String buildEmbeddingText(KnowledgeChunk chunk) {
        return textProcessor.normalizeWhitespace(chunk.postTitle() + "\n\n" + chunk.content());
    }

    private List<ScoredChunk> recallByEmbedding(String question, List<KnowledgeChunk> chunks, int topK, EmbeddingModel embeddingModel,
                                                double minScore, int recallMultiplier) {
        double[] queryEmbedding = embeddingModel.embedQuery(question);
        if (queryEmbedding == null || queryEmbedding.length == 0) {
            return List.of();
        }

        int recallLimit = Math.min(Math.max(topK * recallMultiplier, topK), Math.max(topK, chunks.size()));

        return chunks.stream()
                .filter(chunk -> chunk.embedding() != null && chunk.embedding().length > 0)
                .map(chunk -> new ScoredChunk(chunk, cosineSimilarity(queryEmbedding, chunk.embedding())))
                .filter(scored -> scored.score() >= minScore)
                .sorted(Comparator.comparingDouble(ScoredChunk::score).reversed())
                .limit(recallLimit)
                .toList();
    }

    private List<ScoredChunk> recallByLexical(String question, List<KnowledgeChunk> chunks, int topK,
                                              double minScore, int recallMultiplier) {
        Set<String> queryTokens = tokenize(question);
        String normalizedQuestion = textProcessor.normalizeWhitespace(question).toLowerCase(Locale.ROOT);
        if (queryTokens.isEmpty() && !StringUtils.hasText(normalizedQuestion)) {
            return List.of();
        }

        int recallLimit = Math.min(Math.max(topK * recallMultiplier, topK), Math.max(topK, chunks.size()));

        return chunks.stream()
                .map(chunk -> {
                    String title = textProcessor.safeLower(chunk.postTitle());
                    String content = textProcessor.safeLower(chunk.content());
                    double score = 0D;

                    for (String token : queryTokens) {
                        score += countOccurrences(title, token) * 3.2;
                        score += Math.min(countOccurrences(content, token), 6) * 1.35;
                    }

                    if (StringUtils.hasText(normalizedQuestion) && title.contains(normalizedQuestion)) {
                        score += 4.5;
                    }
                    if (StringUtils.hasText(normalizedQuestion) && content.contains(normalizedQuestion)) {
                        score += 2.2;
                    }

                    double normalized = score / Math.sqrt(Math.max(1, content.length() / 180.0));
                    return new ScoredChunk(chunk, normalized);
                })
                .filter(scored -> scored.score() >= Math.max(0.6D, minScore))
                .sorted(Comparator.comparingDouble(ScoredChunk::score).reversed())
                .limit(recallLimit)
                .toList();
    }

    private Set<String> tokenize(String input) {
        String normalized = textProcessor.safeLower(textProcessor.normalizeWhitespace(input));
        LinkedHashSet<String> tokens = new LinkedHashSet<>();

        Matcher latinMatcher = LATIN_TOKEN_PATTERN.matcher(normalized);
        while (latinMatcher.find()) {
            tokens.add(latinMatcher.group());
        }

        Matcher cjkMatcher = CJK_SEGMENT_PATTERN.matcher(normalized);
        while (cjkMatcher.find()) {
            String segment = cjkMatcher.group();
            for (int i = 0; i < segment.length(); i++) {
                tokens.add(String.valueOf(segment.charAt(i)));
                if (i < segment.length() - 1) {
                    tokens.add(segment.substring(i, i + 2));
                }
            }
        }

        return tokens;
    }

    private int countOccurrences(String content, String token) {
        if (!StringUtils.hasText(content) || !StringUtils.hasText(token)) {
            return 0;
        }
        int count = 0;
        int index = 0;
        while ((index = content.indexOf(token, index)) >= 0) {
            count++;
            index += token.length();
        }
        return count;
    }

    private double cosineSimilarity(double[] left, double[] right) {
        if (left.length == 0 || right.length == 0 || left.length != right.length) {
            return 0D;
        }

        double dot = 0D;
        double leftNorm = 0D;
        double rightNorm = 0D;
        for (int index = 0; index < left.length; index++) {
            dot += left[index] * right[index];
            leftNorm += left[index] * left[index];
            rightNorm += right[index] * right[index];
        }

        if (leftNorm == 0D || rightNorm == 0D) {
            return 0D;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }
}
