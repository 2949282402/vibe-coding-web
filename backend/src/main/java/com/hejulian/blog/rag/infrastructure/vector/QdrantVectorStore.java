package com.hejulian.blog.rag.infrastructure.vector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hejulian.blog.rag.config.RagProperties;
import com.hejulian.blog.rag.domain.model.KnowledgeChunk;
import com.hejulian.blog.rag.domain.model.ScoredChunk;
import com.hejulian.blog.rag.domain.port.VectorStore;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
@Slf4j
public class QdrantVectorStore implements VectorStore {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final RagProperties ragProperties;

    @Value("${QDRANT_API_KEY:}")
    private String qdrantApiKey;

    @Override
    public boolean isVectorStoreConfigured() {
        RagProperties.Qdrant qdrant = ragProperties.getQdrant();
        return qdrant.isEnabled() && StringUtils.hasText(qdrant.getBaseUrl()) && StringUtils.hasText(qdrant.getCollection());
    }

    @Override
    public boolean isCollectionReady() {
        if (!isVectorStoreConfigured()) {
            return false;
        }

        try {
            client().get()
                    .uri("/collections/{collection}", ragProperties.getQdrant().getCollection())
                    .retrieve()
                    .body(JsonNode.class);
            return true;
        } catch (HttpClientErrorException.NotFound ex) {
            return false;
        } catch (Exception ex) {
            log.warn("Failed to check Qdrant collection: {}", ex.getMessage());
            return false;
        }
    }

    @Override
    public void deleteCollection() {
        if (!isVectorStoreConfigured() || !isCollectionReady()) {
            return;
        }

        client().delete()
                .uri("/collections/{collection}", ragProperties.getQdrant().getCollection())
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public void recreateCollection(int vectorSize) {
        if (!isVectorStoreConfigured() || vectorSize <= 0) {
            return;
        }

        String collection = ragProperties.getQdrant().getCollection();
        if (isCollectionReady()) {
            deleteCollection();
        }

        client().put()
                .uri("/collections/{collection}", collection)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "vectors", Map.of(
                                "size", vectorSize,
                                "distance", "Cosine"
                        ),
                        "on_disk_payload", ragProperties.getQdrant().isOnDiskPayload()
                ))
                .retrieve()
                .toBodilessEntity();

        createPayloadIndex("post_id", "integer");
        createPayloadIndex("post_slug", "keyword");
    }

    @Override
    public void upsertChunks(List<KnowledgeChunk> chunks) {
        if (!isVectorStoreConfigured() || chunks.isEmpty()) {
            return;
        }

        List<Map<String, Object>> points = chunks.stream()
                .filter(chunk -> chunk.embedding() != null && chunk.embedding().length > 0)
                .map(this::toPoint)
                .toList();
        if (points.isEmpty()) {
            return;
        }

        client().put()
                .uri("/collections/{collection}/points?wait=true", ragProperties.getQdrant().getCollection())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("points", points))
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public void deleteChunksByPostId(Long postId) {
        if (!isVectorStoreConfigured() || postId == null || !isCollectionReady()) {
            return;
        }

        client().post()
                .uri("/collections/{collection}/points/delete?wait=true", ragProperties.getQdrant().getCollection())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "filter", Map.of(
                                "must", List.of(Map.of(
                                        "key", "post_id",
                                        "match", Map.of("value", postId)
                                ))
                        )
                ))
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public List<ScoredChunk> search(double[] queryVector, int limit, double minScore) {
        if (!isVectorStoreConfigured() || !isCollectionReady() || queryVector == null || queryVector.length == 0) {
            return List.of();
        }

        try {
            JsonNode response = client().post()
                    .uri("/collections/{collection}/points/query", ragProperties.getQdrant().getCollection())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "query", queryVector,
                            "limit", limit,
                            "score_threshold", minScore,
                            "with_payload", true
                    ))
                    .retrieve()
                    .body(JsonNode.class);

            JsonNode points = response == null ? null : response.path("result").path("points");
            if (points == null || !points.isArray()) {
                return List.of();
            }

            List<ScoredChunk> matches = new java.util.ArrayList<>();
            for (JsonNode point : points) {
                matches.add(toScoredChunk(point));
            }
            return matches;
        } catch (Exception ex) {
            log.warn("Qdrant search failed, fallback to lexical retrieval: {}", ex.getMessage());
            return List.of();
        }
    }

    private Map<String, Object> toPoint(KnowledgeChunk chunk) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("post_id", chunk.postId());
        payload.put("post_title", chunk.postTitle());
        payload.put("post_slug", chunk.postSlug());
        payload.put("chunk_index", chunk.chunkIndex());
        payload.put("content", chunk.content());
        payload.put("published_at", chunk.publishedAt() == null ? null : chunk.publishedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        payload.put("embedding_model", chunk.embeddingModel());

        Map<String, Object> point = new HashMap<>();
        point.put("id", pointId(chunk.postId(), chunk.chunkIndex()));
        point.put("vector", chunk.embedding());
        point.put("payload", payload);
        return point;
    }

    private ScoredChunk toScoredChunk(JsonNode pointNode) {
        JsonNode payload = pointNode.path("payload");
        String publishedAtText = payload.path("published_at").asText("");
        LocalDateTime publishedAt = StringUtils.hasText(publishedAtText) ? LocalDateTime.parse(publishedAtText) : null;

        KnowledgeChunk chunk = new KnowledgeChunk(
                payload.path("post_id").asLong(),
                payload.path("post_title").asText(""),
                payload.path("post_slug").asText(""),
                payload.path("chunk_index").asInt(),
                payload.path("content").asText(""),
                publishedAt,
                null,
                payload.path("embedding_model").asText("")
        );
        return new ScoredChunk(chunk, pointNode.path("score").asDouble(0D));
    }

    private void createPayloadIndex(String fieldName, String fieldSchema) {
        client().put()
                .uri("/collections/{collection}/index?wait=true", ragProperties.getQdrant().getCollection())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "field_name", fieldName,
                        "field_schema", fieldSchema
                ))
                .retrieve()
                .toBodilessEntity();
    }

    private String pointId(Long postId, int chunkIndex) {
        return UUID.nameUUIDFromBytes((postId + ":" + chunkIndex).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
    }

    private RestClient client() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(ragProperties.getQdrant().getBaseUrl())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE);

        String apiKey = resolveApiKey();
        if (StringUtils.hasText(apiKey)) {
            builder.defaultHeader("api-key", apiKey);
        }
        return builder.build();
    }

    private String resolveApiKey() {
        String configured = ragProperties.getQdrant().getApiKey();
        return StringUtils.hasText(configured) ? configured.trim() : qdrantApiKey;
    }
}
