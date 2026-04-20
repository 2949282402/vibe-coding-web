package com.hejulian.blog.rag.infrastructure.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hejulian.blog.rag.config.RagProperties;
import com.hejulian.blog.rag.domain.model.RerankResult;
import com.hejulian.blog.rag.domain.model.WebSearchAnswer;
import com.hejulian.blog.rag.domain.model.WebSearchSource;
import com.hejulian.blog.rag.domain.port.ChatModel;
import com.hejulian.blog.rag.domain.port.EmbeddingModel;
import com.hejulian.blog.rag.domain.port.RerankModel;
import jakarta.annotation.Nullable;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
@Slf4j
public class DashScopeModelGateway implements EmbeddingModel, RerankModel, ChatModel {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    private final RagProperties ragProperties;

    @org.springframework.beans.factory.annotation.Value("${DASHSCOPE_API_KEY:}")
    private String dashscopeApiKey;

    @Override
    public boolean isChatConfigured() {
        return generatorConfigured();
    }

    @Override
    public boolean supportsWebSearch() {
        return webSearchConfigured();
    }

    @Override
    public String chatModelName() {
        return ragProperties.getGenerator().getModel();
    }

    @Override
    public boolean isEmbeddingConfigured() {
        return embeddingConfigured();
    }

    @Override
    public boolean isRerankConfigured() {
        return rerankConfigured();
    }

    private boolean embeddingConfigured() {
        RagProperties.Embedding embedding = ragProperties.getEmbedding();
        return embedding.isEnabled()
                && StringUtils.hasText(embedding.getBaseUrl())
                && StringUtils.hasText(resolveApiKey(embedding.getApiKey()));
    }

    private boolean rerankConfigured() {
        RagProperties.Rerank rerank = ragProperties.getRerank();
        return rerank.isEnabled()
                && StringUtils.hasText(rerank.getBaseUrl())
                && StringUtils.hasText(resolveApiKey(rerank.getApiKey()));
    }

    private boolean generatorConfigured() {
        RagProperties.Generator generator = ragProperties.getGenerator();
        return generator.isEnabled()
                && StringUtils.hasText(generator.getBaseUrl())
                && StringUtils.hasText(generator.getModel())
                && StringUtils.hasText(resolveApiKey(generator.getApiKey()));
    }

    private boolean webSearchConfigured() {
        RagProperties.WebSearch webSearch = ragProperties.getWebSearch();
        return webSearch.isEnabled()
                && StringUtils.hasText(webSearch.getBaseUrl())
                && StringUtils.hasText(resolveWebSearchApiKey())
                && StringUtils.hasText(resolveWebSearchModel());
    }

    @Override
    public String embeddingModelName() {
        return ragProperties.getEmbedding().getModel();
    }

    @Override
    public List<double[]> embedDocuments(List<String> texts) {
        if (!embeddingConfigured() || texts.isEmpty()) {
            return List.of();
        }

        List<double[]> all = new ArrayList<>();
        int batchSize = Math.min(Math.max(1, ragProperties.getEmbedding().getBatchSize()), 10);

        for (int start = 0; start < texts.size(); start += batchSize) {
            int end = Math.min(start + batchSize, texts.size());
            List<String> batch = texts.subList(start, end);
            all.addAll(embedBatch(batch));
        }

        return all;
    }

    @Override
    public List<RerankResult> rerank(String query, List<String> documents, int topN) {
        if (!rerankConfigured() || !StringUtils.hasText(query) || documents.isEmpty()) {
            return List.of();
        }

        try {
            RagProperties.Rerank rerank = ragProperties.getRerank();
            RestClient client = RestClient.builder()
                    .baseUrl(rerank.getBaseUrl())
                    .defaultHeader("Authorization", "Bearer " + resolveApiKey(rerank.getApiKey()))
                    .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .build();

            JsonNode response = client.post()
                    .uri("/reranks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "model", rerank.getModel(),
                            "documents", documents,
                            "query", query,
                            "top_n", Math.min(topN, documents.size()),
                            "instruct", rerank.getInstruct()
                    ))
                    .retrieve()
                    .body(JsonNode.class);

            JsonNode results = response == null ? null : response.path("output").path("results");
            if (results == null || !results.isArray()) {
                return List.of();
            }

            List<RerankResult> reranked = new ArrayList<>();
            for (JsonNode item : results) {
                reranked.add(new RerankResult(
                        item.path("index").asInt(-1),
                        item.path("relevance_score").asDouble(0D)
                ));
            }
            return reranked;
        } catch (Exception ex) {
            log.warn("DashScope rerank failed: {}", ex.getMessage());
            return List.of();
        }
    }

    @Override
    @Nullable
    public String generate(String systemPrompt, String userPrompt, double temperature) {
        if (!generatorConfigured()) {
            return null;
        }

        try {
            RagProperties.Generator generator = ragProperties.getGenerator();
            RestClient client = RestClient.builder()
                    .baseUrl(generator.getBaseUrl())
                    .defaultHeader("Authorization", "Bearer " + resolveApiKey(generator.getApiKey()))
                    .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .build();

            List<Map<String, String>> messages = List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)
            );

            JsonNode response = client.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "model", generator.getModel(),
                            "temperature", temperature,
                            "messages", messages
                    ))
                    .retrieve()
                    .body(JsonNode.class);

            return extractChatCompletionContent(response);
        } catch (Exception ex) {
            log.warn("DashScope generation failed: {}", ex.getMessage());
            return null;
        }
    }

    @Override
    @Nullable
    public String streamGenerate(String systemPrompt, String userPrompt, double temperature, @Nullable Consumer<String> deltaConsumer) {
        if (!generatorConfigured()) {
            return null;
        }

        StringBuilder answer = new StringBuilder();

        try {
            RagProperties.Generator generator = ragProperties.getGenerator();
            String requestBody = OBJECT_MAPPER.writeValueAsString(Map.of(
                    "model", generator.getModel(),
                    "temperature", temperature,
                    "stream", true,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)
                    )
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(resolveUrl(generator.getBaseUrl(), "/chat/completions")))
                    .timeout(Duration.ofSeconds(generator.getTimeoutSeconds()))
                    .header("Authorization", "Bearer " + resolveApiKey(generator.getApiKey()))
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .header("Accept", "text/event-stream")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<InputStream> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() >= 400) {
                String errorBody = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                throw new IllegalStateException("DashScope streaming failed: " + errorBody);
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (!StringUtils.hasText(trimmed) || !trimmed.startsWith("data:")) {
                        continue;
                    }

                    String payload = trimmed.substring(5).trim();
                    if ("[DONE]".equals(payload)) {
                        break;
                    }

                    JsonNode node = OBJECT_MAPPER.readTree(payload);
                    String delta = extractChatDelta(node);
                    if (StringUtils.hasText(delta)) {
                        answer.append(delta);
                        if (deltaConsumer != null) {
                            deltaConsumer.accept(delta);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            String partial = answer.toString().trim();
            if (StringUtils.hasText(partial)) {
                log.warn("DashScope streaming ended early, returning partial answer: {}", ex.getMessage());
                return partial;
            }
            log.warn("DashScope streaming failed: {}", ex.getMessage());
            return null;
        }

        String finalAnswer = answer.toString().trim();
        return StringUtils.hasText(finalAnswer) ? finalAnswer : null;
    }

    @Override
    @Nullable
    public WebSearchAnswer generateWithWebSearch(String systemPrompt, String userPrompt, double temperature) {
        if (!webSearchConfigured()) {
            return null;
        }

        try {
            RagProperties.WebSearch webSearch = ragProperties.getWebSearch();
            RestClient client = RestClient.builder()
                    .baseUrl(webSearch.getBaseUrl())
                    .defaultHeader("Authorization", "Bearer " + resolveWebSearchApiKey())
                    .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .build();

            Map<String, Object> body = new HashMap<>();
            body.put("model", resolveWebSearchModel());
            body.put("input", Map.of(
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)
                    )
            ));
            body.put("parameters", buildWebSearchParameters(temperature));

            JsonNode response = client.post()
                    .uri("/api/v1/services/aigc/text-generation/generation")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null) {
                return null;
            }

            JsonNode output = response.path("output");
            String answer = extractDashScopeMessageContent(output.path("choices").path(0).path("message"));
            List<WebSearchSource> sources = extractWebSearchSources(output.path("search_info").path("search_results"));
            if (!StringUtils.hasText(answer) && sources.isEmpty()) {
                return null;
            }
            return new WebSearchAnswer(answer == null ? "" : answer.trim(), sources);
        } catch (Exception ex) {
            log.warn("DashScope web search generation failed: {}", ex.getMessage());
            return null;
        }
    }

    private List<double[]> embedBatch(List<String> batch) {
        try {
            RagProperties.Embedding embedding = ragProperties.getEmbedding();
            RestClient client = RestClient.builder()
                    .baseUrl(embedding.getBaseUrl())
                    .defaultHeader("Authorization", "Bearer " + resolveApiKey(embedding.getApiKey()))
                    .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .build();

            Map<String, Object> body = new HashMap<>();
            body.put("model", embedding.getModel());
            body.put("input", batch.size() == 1 ? batch.get(0) : batch);
            body.put("encoding_format", "float");
            if (embedding.getDimensions() > 0) {
                body.put("dimensions", embedding.getDimensions());
            }

            JsonNode response = client.post()
                    .uri("/embeddings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            JsonNode data = response == null ? null : response.path("data");
            if (data == null || !data.isArray()) {
                return List.of();
            }

            List<JsonNode> rows = new ArrayList<>();
            data.forEach(rows::add);
            return rows.stream()
                    .sorted(Comparator.comparingInt(node -> node.path("index").asInt()))
                    .map(node -> toVector(node.path("embedding")))
                    .toList();
        } catch (Exception ex) {
            log.warn("DashScope embedding failed: {}", ex.getMessage());
            return List.of();
        }
    }

    private double[] toVector(JsonNode embeddingNode) {
        if (embeddingNode == null || !embeddingNode.isArray()) {
            return new double[0];
        }

        double[] vector = new double[embeddingNode.size()];
        for (int index = 0; index < embeddingNode.size(); index++) {
            vector[index] = embeddingNode.get(index).asDouble();
        }
        return vector;
    }

    @Nullable
    private String extractChatCompletionContent(@Nullable JsonNode response) {
        if (response == null) {
            return null;
        }

        JsonNode contentNode = response.path("choices").path(0).path("message").path("content");
        return extractContentNode(contentNode);
    }

    private String extractChatDelta(JsonNode response) {
        JsonNode contentNode = response.path("choices").path(0).path("delta").path("content");
        return extractContentNode(contentNode);
    }

    private String extractDashScopeMessageContent(JsonNode messageNode) {
        if (messageNode == null || messageNode.isMissingNode() || messageNode.isNull()) {
            return "";
        }
        JsonNode contentNode = messageNode.path("content");
        return extractContentNode(contentNode);
    }

    private String extractContentNode(JsonNode contentNode) {
        if (contentNode == null || contentNode.isMissingNode() || contentNode.isNull()) {
            return "";
        }
        if (contentNode.isTextual()) {
            return contentNode.asText("");
        }
        if (contentNode.isArray()) {
            StringBuilder builder = new StringBuilder();
            for (JsonNode item : contentNode) {
                String text = item.path("text").asText("");
                if (StringUtils.hasText(text)) {
                    builder.append(text);
                }
            }
            return builder.toString();
        }
        return "";
    }

    private Map<String, Object> buildWebSearchParameters(double temperature) {
        RagProperties.WebSearch webSearch = ragProperties.getWebSearch();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("result_format", "message");
        parameters.put("temperature", temperature);
        parameters.put("enable_search", true);

        Map<String, Object> searchOptions = new HashMap<>();
        searchOptions.put("enable_source", webSearch.isEnableSource());
        searchOptions.put("enable_citation", webSearch.isEnableCitation());
        searchOptions.put("forced_search", webSearch.isForcedSearch());
        if (StringUtils.hasText(webSearch.getCitationFormat())) {
            searchOptions.put("citation_format", webSearch.getCitationFormat());
        }
        if (StringUtils.hasText(webSearch.getSearchStrategy())) {
            searchOptions.put("search_strategy", webSearch.getSearchStrategy());
        }
        parameters.put("search_options", searchOptions);
        return parameters;
    }

    private List<WebSearchSource> extractWebSearchSources(JsonNode searchResultsNode) {
        if (searchResultsNode == null || !searchResultsNode.isArray()) {
            return List.of();
        }

        List<WebSearchSource> sources = new ArrayList<>();
        for (JsonNode item : searchResultsNode) {
            sources.add(new WebSearchSource(
                    item.path("index").asInt(sources.size() + 1),
                    item.path("title").asText(""),
                    item.path("url").asText(""),
                    item.path("site_name").asText(""),
                    item.path("icon").asText("")
            ));
        }
        return List.copyOf(sources);
    }

    private String resolveApiKey(String primaryApiKey) {
        return StringUtils.hasText(primaryApiKey) ? primaryApiKey.trim() : dashscopeApiKey;
    }

    private String resolveWebSearchApiKey() {
        return resolveApiKey(ragProperties.getWebSearch().getApiKey());
    }

    private String resolveWebSearchModel() {
        String configured = ragProperties.getWebSearch().getModel();
        if (StringUtils.hasText(configured)) {
            return configured.trim();
        }
        return ragProperties.getGenerator().getModel();
    }

    private String resolveUrl(String baseUrl, String path) {
        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return normalizedBaseUrl + normalizedPath;
    }
}
