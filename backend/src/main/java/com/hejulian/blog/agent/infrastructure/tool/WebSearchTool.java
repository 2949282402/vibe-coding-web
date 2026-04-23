package com.hejulian.blog.agent.infrastructure.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hejulian.blog.agent.domain.enums.ToolPermissionLevel;
import com.hejulian.blog.agent.domain.tool.AgentTool;
import com.hejulian.blog.agent.domain.tool.AgentToolContext;
import com.hejulian.blog.agent.domain.tool.ToolExecutionResult;
import com.hejulian.blog.exception.BusinessException;
import com.hejulian.blog.rag.domain.model.WebSearchAnswer;
import com.hejulian.blog.rag.domain.model.WebSearchSource;
import com.hejulian.blog.rag.infrastructure.client.DashScopeModelGateway;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class WebSearchTool implements AgentTool {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final DashScopeModelGateway modelGateway;

    public WebSearchTool(DashScopeModelGateway modelGateway) {
        this.modelGateway = modelGateway;
    }

    @Override
    public String name() {
        return "web_search";
    }

    @Override
    public String description() {
        return "Search the web through the configured Qwen compatible model";
    }

    @Override
    public JsonNode inputSchema() {
        ObjectNode schema = OBJECT_MAPPER.createObjectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("query").put("type", "string");
        return schema;
    }

    @Override
    public ToolPermissionLevel permissionLevel() {
        return ToolPermissionLevel.READ;
    }

    @Override
    public void validateInput(JsonNode args) {
        if (args == null || !args.hasNonNull("query") || args.get("query").asText().trim().isBlank()) {
            throw new BusinessException("query is required");
        }
    }

    @Override
    public ToolExecutionResult execute(AgentToolContext context, JsonNode args) {
        String query = args.get("query").asText().trim();
        WebSearchAnswer answer = modelGateway.generateWithWebSearch(
                "You are a web research tool. Return a concise research brief with source-aware facts.",
                query,
                0.2D
        );
        if (answer == null || (!StringUtils.hasText(answer.answer()) && answer.sources().isEmpty())) {
            throw new BusinessException("Web search returned no result");
        }

        String sources = answer.sources().stream()
                .map(this::formatSource)
                .collect(Collectors.joining(" | "));
        String payload = answer.answer();
        if (StringUtils.hasText(sources)) {
            payload = payload + "\nSources: " + sources;
        }
        return new ToolExecutionResult(true, "Web search completed", payload.trim());
    }

    private String formatSource(WebSearchSource source) {
        String title = StringUtils.hasText(source.title()) ? source.title() : source.siteName();
        String normalizedTitle = StringUtils.hasText(title) ? title : "source";
        return "[%d] %s %s".formatted(source.index(), normalizedTitle, source.url());
    }
}
