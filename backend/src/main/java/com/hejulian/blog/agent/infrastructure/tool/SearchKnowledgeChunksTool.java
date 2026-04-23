package com.hejulian.blog.agent.infrastructure.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hejulian.blog.agent.domain.enums.ToolPermissionLevel;
import com.hejulian.blog.agent.domain.tool.AgentTool;
import com.hejulian.blog.agent.domain.tool.AgentToolContext;
import com.hejulian.blog.agent.domain.tool.ToolExecutionResult;
import com.hejulian.blog.entity.RagChunk;
import com.hejulian.blog.exception.BusinessException;
import com.hejulian.blog.mapper.RagChunkMapper;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class SearchKnowledgeChunksTool implements AgentTool {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final RagChunkMapper ragChunkMapper;

    public SearchKnowledgeChunksTool(RagChunkMapper ragChunkMapper) {
        this.ragChunkMapper = ragChunkMapper;
    }

    @Override
    public String name() {
        return "search_knowledge_chunks";
    }

    @Override
    public String description() {
        return "Search knowledge chunk snippets from indexed blog content";
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
        if (args == null || !args.hasNonNull("query") || args.get("query").asText().trim().isEmpty()) {
            throw new BusinessException("query is required");
        }
    }

    @Override
    public ToolExecutionResult execute(AgentToolContext context, JsonNode args) {
        String keyword = args.get("query").asText().toLowerCase(Locale.ROOT).trim();
        int limit = args.hasNonNull("limit") ? Math.max(1, Math.min(args.get("limit").asInt(), 6)) : 4;
        var chunks = ragChunkMapper.selectAll();
        var matched = chunks.stream()
                .filter(chunk -> chunk.getContent() != null && chunk.getContent().toLowerCase(Locale.ROOT).contains(keyword))
                .limit(limit)
                .map(RagChunk::getContent)
                .map(content -> content.trim().replaceAll("\\s+", " "))
                .map(content -> content.length() > 220 ? content.substring(0, 220) + "..." : content)
                .collect(Collectors.toList());
        return new ToolExecutionResult(true, "Knowledge chunks matched: " + matched.size(), matched.toString());
    }
}
