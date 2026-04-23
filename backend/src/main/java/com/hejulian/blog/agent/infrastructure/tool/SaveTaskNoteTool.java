package com.hejulian.blog.agent.infrastructure.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hejulian.blog.agent.domain.enums.MemoryScope;
import com.hejulian.blog.agent.domain.enums.MemoryType;
import com.hejulian.blog.agent.domain.enums.ToolPermissionLevel;
import com.hejulian.blog.agent.domain.tool.AgentTool;
import com.hejulian.blog.agent.domain.tool.AgentToolContext;
import com.hejulian.blog.agent.domain.tool.ToolExecutionResult;
import com.hejulian.blog.agent.entity.AgentMemory;
import com.hejulian.blog.agent.exception.AgentToolRuntimeException;
import com.hejulian.blog.exception.BusinessException;
import com.hejulian.blog.agent.mapper.AgentMemoryMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
public class SaveTaskNoteTool implements AgentTool {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final AgentMemoryMapper memoryMapper;

    public SaveTaskNoteTool(AgentMemoryMapper memoryMapper) {
        this.memoryMapper = memoryMapper;
    }

    @Override
    public String name() {
        return "save_task_note";
    }

    @Override
    public String description() {
        return "Save a user-visible task note/draft in memory store";
    }

    @Override
    public JsonNode inputSchema() {
        ObjectNode schema = OBJECT_MAPPER.createObjectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("content").put("type", "string");
        properties.putObject("topicKey").put("type", "string");
        properties.putObject("memoryType").put("type", "string");
        properties.putObject("confidenceScore").put("type", "number");
        properties.putObject("sourceType").put("type", "string");
        properties.putObject("sourceRefId").put("type", "integer");
        return schema;
    }

    @Override
    public ToolPermissionLevel permissionLevel() {
        return ToolPermissionLevel.WRITE;
    }

    @Override
    public void validateInput(JsonNode args) {
        if (args == null || !args.hasNonNull("content") || !args.hasNonNull("topicKey") || !args.hasNonNull("memoryType")) {
            throw new BusinessException("content, topicKey, memoryType are required");
        }
        if (!contextAwareConfidence(args)) {
            throw new BusinessException("confidenceScore must be between 0 and 1");
        }
    }

    @Override
    public ToolExecutionResult execute(AgentToolContext context, JsonNode args) {
        try {
            String content = args.get("content").asText().trim();
            String topicKey = args.get("topicKey").asText().trim();
            String memoryType = args.get("memoryType").asText().trim();
            String sourceType = args.hasNonNull("sourceType") ? args.get("sourceType").asText().trim() : "agent";
            long sourceRefId = args.hasNonNull("sourceRefId") ? args.get("sourceRefId").asLong() : context.taskId();
            BigDecimal confidence = BigDecimal.valueOf(args.hasNonNull("confidenceScore") ? args.get("confidenceScore").asDouble() : 0.8);
            if (confidence.signum() < 0) {
                confidence = BigDecimal.ZERO;
            }
            if (confidence.compareTo(BigDecimal.ONE) > 0) {
                confidence = BigDecimal.ONE;
            }

            AgentMemory memory = new AgentMemory();
            memory.setUserId(context.userId());
            memory.setMemoryScope(MemoryScope.TASK);
            memory.setTopicKey(topicKey);
            memory.setMemoryType(MemoryType.valueOf(memoryType));
            memory.setContent(content);
            memory.setContentSummary(summarize(content, 180));
            memory.setConfidenceScore(confidence);
            memory.setSourceType(sourceType);
            memory.setSourceRefId(sourceRefId);
            memory.setPinned(Boolean.FALSE);
            memory.setDeleted(Boolean.FALSE);
            memory.setLastHitAt(LocalDateTime.now());
            memoryMapper.insert(memory);
            return new ToolExecutionResult(true, "Memory saved", "saved:" + memory.getId());
        } catch (IllegalArgumentException ex) {
            throw new AgentToolRuntimeException("Invalid memoryType", ex);
        } catch (Exception ex) {
            throw new AgentToolRuntimeException("Save task note failed", ex);
        }
    }

    private boolean contextAwareConfidence(JsonNode args) {
        if (!args.hasNonNull("confidenceScore")) {
            return true;
        }
        double value = args.get("confidenceScore").asDouble();
        return value >= 0D && value <= 1D;
    }

    private String summarize(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.length() > maxLength ? normalized.substring(0, maxLength).trim() + "..." : normalized;
    }
}
