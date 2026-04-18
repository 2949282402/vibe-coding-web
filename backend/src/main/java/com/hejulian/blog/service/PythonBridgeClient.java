package com.hejulian.blog.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
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
import java.util.Map;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class PythonBridgeClient {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    @Value("${blog.rag.python-bridge.enabled:false}")
    private boolean enabled;

    @Value("${blog.rag.python-bridge.base-url:http://host.docker.internal:8090}")
    private String baseUrl;

    @Value("${blog.rag.python-bridge.timeout-seconds:600}")
    private int timeoutSeconds;

    public boolean isConfigured() {
        return enabled && StringUtils.hasText(baseUrl);
    }

    @Nullable
    public String generate(String model, String prompt, double temperature) {
        if (!isConfigured() || !StringUtils.hasText(model) || !StringUtils.hasText(prompt)) {
            return null;
        }

        try {
            RestClient client = RestClient.builder()
                    .baseUrl(baseUrl)
                    .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .build();

            JsonNode response = client.post()
                    .uri("/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "model", model,
                            "prompt", prompt,
                            "temperature", temperature,
                            "timeoutSeconds", timeoutSeconds
                    ))
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null) {
                return null;
            }

            String content = response.path("content").asText("");
            return StringUtils.hasText(content) ? content.trim() : null;
        } catch (Exception ex) {
            log.warn("Python bridge fallback failed: {}", ex.getMessage());
            return null;
        }
    }

    @Nullable
    public String streamGenerate(String model, String prompt, double temperature, @Nullable Consumer<String> deltaConsumer) {
        if (!isConfigured() || !StringUtils.hasText(model) || !StringUtils.hasText(prompt)) {
            return null;
        }

        StringBuilder answer = new StringBuilder();

        try {
            String requestBody = OBJECT_MAPPER.writeValueAsString(Map.of(
                    "model", model,
                    "prompt", prompt,
                    "temperature", temperature,
                    "timeoutSeconds", timeoutSeconds
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(resolveUrl("/generate/stream")))
                    .timeout(Duration.ofSeconds(timeoutSeconds + 30L))
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<InputStream> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() >= 400) {
                String errorBody = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                throw new IllegalStateException("Python bridge stream failed: " + errorBody);
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (!StringUtils.hasText(trimmed)) {
                        continue;
                    }

                    JsonNode node = OBJECT_MAPPER.readTree(trimmed);
                    String error = node.path("error").asText("");
                    if (StringUtils.hasText(error)) {
                        throw new IllegalStateException(error);
                    }

                    String delta = node.path("delta").asText("");
                    if (StringUtils.hasText(delta)) {
                        answer.append(delta);
                        if (deltaConsumer != null) {
                            deltaConsumer.accept(delta);
                        }
                    }

                    if (node.path("done").asBoolean(false)) {
                        String finalContent = node.path("content").asText("");
                        if (!StringUtils.hasText(answer.toString()) && StringUtils.hasText(finalContent)) {
                            answer.append(finalContent);
                        }
                        break;
                    }
                }
            }
        } catch (Exception ex) {
            String partial = answer.toString().trim();
            if (StringUtils.hasText(partial)) {
                log.warn("Python bridge stream ended early, returning partial answer: {}", ex.getMessage());
                return partial;
            }
            log.warn("Python bridge stream failed: {}", ex.getMessage());
            return null;
        }

        String content = answer.toString().trim();
        return StringUtils.hasText(content) ? content : null;
    }

    private String resolveUrl(String path) {
        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return normalizedBaseUrl + normalizedPath;
    }
}
