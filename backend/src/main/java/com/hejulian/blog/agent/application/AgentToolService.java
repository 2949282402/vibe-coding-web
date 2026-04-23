package com.hejulian.blog.agent.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hejulian.blog.agent.domain.enums.ToolPermissionLevel;
import com.hejulian.blog.agent.domain.tool.AgentTool;
import com.hejulian.blog.agent.domain.tool.AgentToolContext;
import com.hejulian.blog.agent.domain.tool.ToolExecutionResult;
import com.hejulian.blog.agent.entity.AgentToolCall;
import com.hejulian.blog.agent.exception.AgentToolRuntimeException;
import com.hejulian.blog.agent.infrastructure.tool.AgentToolRegistry;
import com.hejulian.blog.agent.mapper.AgentToolCallMapper;
import com.hejulian.blog.exception.BusinessException;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AgentToolService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String NO_ARGS_JSON = "{}";
    private final AgentToolRegistry toolRegistry;
    private final AgentToolCallMapper toolCallMapper;

    public AgentToolService(AgentToolRegistry toolRegistry, AgentToolCallMapper toolCallMapper) {
        this.toolRegistry = toolRegistry;
        this.toolCallMapper = toolCallMapper;
    }

    public ToolExecutionResult callTool(
            Long userId,
            Long taskId,
            Long stepId,
            String sessionId,
            boolean allowDraftWrite,
            boolean admin,
            String toolName,
            String argumentsJson
    ) {
        AgentTool tool = toolRegistry.get(toolName);
        if (tool.permissionLevel() == ToolPermissionLevel.WRITE && !admin && !allowDraftWrite) {
            throw new BusinessException("You do not have permission to execute write tool: " + toolName);
        }

        long startedAt = Instant.now().toEpochMilli();
        AgentToolCall toolCall = new AgentToolCall();
        toolCall.setTaskId(taskId);
        toolCall.setStepId(stepId);
        toolCall.setToolName(toolName);
        toolCall.setPermissionLevel(tool.permissionLevel());
        toolCall.setRequestJson(safeJson(argumentsJson));
        toolCall.setSuccess(false);
        toolCallMapper.insert(toolCall);

        try {
            JsonNode args = parseArgs(argumentsJson);
            tool.validateInput(args);
            ToolExecutionResult result = tool.execute(new AgentToolContext(taskId, stepId, userId, sessionId, allowDraftWrite, admin), args);
            toolCall.setSuccess(true);
            toolCall.setResponseSummary(result.summary());
            toolCall.setErrorMessage(null);
            toolCall.setLatencyMs(System.currentTimeMillis() - startedAt);
            updateCall(toolCall);
            return result;
        } catch (BusinessException ex) {
            toolCall.setSuccess(false);
            toolCall.setResponseSummary("failure");
            toolCall.setErrorMessage(ex.getMessage());
            toolCall.setLatencyMs(System.currentTimeMillis() - startedAt);
            updateCall(toolCall);
            throw ex;
        } catch (Exception ex) {
            toolCall.setSuccess(false);
            toolCall.setResponseSummary("failure");
            toolCall.setErrorMessage(ex.getMessage());
            toolCall.setLatencyMs(System.currentTimeMillis() - startedAt);
            updateCall(toolCall);
            throw new AgentToolRuntimeException("Tool execution failed", ex);
        }
    }

    public List<String> listTools() {
        return toolRegistry.names();
    }

    private void updateCall(AgentToolCall toolCall) {
        toolCallMapper.update(toolCall.getId(), toolCall.getSuccess(), toolCall.getResponseSummary(), toolCall.getErrorMessage());
    }

    private String safeJson(String raw) {
        return (raw == null || raw.isBlank()) ? NO_ARGS_JSON : raw;
    }

    private JsonNode parseArgs(String argumentsJson) {
        try {
            return argumentsJson == null || argumentsJson.isBlank()
                    ? new ObjectNode(OBJECT_MAPPER.getNodeFactory())
                    : OBJECT_MAPPER.readTree(argumentsJson);
        } catch (Exception ex) {
            throw new BusinessException("Invalid tool arguments");
        }
    }
}

