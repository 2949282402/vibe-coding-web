package com.hejulian.blog.agent.infrastructure.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hejulian.blog.agent.domain.enums.ToolPermissionLevel;
import com.hejulian.blog.agent.domain.tool.AgentTool;
import com.hejulian.blog.agent.domain.tool.AgentToolContext;
import com.hejulian.blog.agent.domain.tool.ToolExecutionResult;
import com.hejulian.blog.agent.mapper.AgentMemoryMapper;
import com.hejulian.blog.exception.BusinessException;
import org.springframework.stereotype.Component;

@Component
public class GetUserMemorySummaryTool implements AgentTool {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final AgentMemoryMapper memoryMapper;

    public GetUserMemorySummaryTool(AgentMemoryMapper memoryMapper) {
        this.memoryMapper = memoryMapper;
    }

    @Override
    public String name() {
        return "get_user_memory_summary";
    }

    @Override
    public String description() {
        return "Retrieve recent user memory snapshots";
    }

    @Override
    public JsonNode inputSchema() {
        ObjectNode schema = OBJECT_MAPPER.createObjectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("query").put("type", "string");
        properties.putObject("top").put("type", "integer");
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
        String keyword = args.get("query").asText();
        int top = args.hasNonNull("top") ? Math.max(1, Math.min(args.get("top").asInt(), 8)) : 4;
        var memories = memoryMapper.selectByUserId(context.userId(), "USER", keyword, null, 0, top);
        if (memories.isEmpty()) {
            return new ToolExecutionResult(false, "No memory found", "[]");
        }
        String summary = memories.stream()
                .map(memory -> memory.getContentSummary())
                .reduce("", (acc, value) -> acc.isBlank() ? value : acc + " | " + value);
        return new ToolExecutionResult(true, "Memory summary size " + memories.size(), summary);
    }
}
