package com.hejulian.blog.agent.dto;

import com.hejulian.blog.common.PageResponse;
import com.hejulian.blog.agent.domain.enums.AgentRole;
import com.hejulian.blog.agent.domain.enums.EventType;
import com.hejulian.blog.agent.domain.enums.ExecutionMode;
import com.hejulian.blog.agent.domain.enums.MemoryScope;
import com.hejulian.blog.agent.domain.enums.MemoryType;
import com.hejulian.blog.agent.domain.enums.SearchScope;
import com.hejulian.blog.agent.domain.enums.TaskStatus;
import com.hejulian.blog.agent.domain.enums.TaskType;
import com.hejulian.blog.agent.domain.enums.ToolPermissionLevel;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class AgentDtos {

    private AgentDtos() {
    }

    public record AgentTaskCreateRequest(
            @NotBlank(message = "Goal is required")
            String goal,
            String title,
            String sessionId,
            @NotNull(message = "Task type is required")
            TaskType taskType,
            ExecutionMode executionMode,
            SearchScope searchScope,
            Boolean allowDraftWrite
    ) {
    }

    public record AgentTaskQueryRequest(
            String status,
            String keyword,
            @Min(value = 1, message = "Page must be 1 or greater")
            int page,
            @Min(value = 1, message = "Page size must be 1 or greater")
            int pageSize
    ) {
    }

    public record AgentTaskCancelRequest(
            String reason
    ) {
    }

    public record AgentTaskRetryRequest(
            String reason
    ) {
    }

    public record AgentTaskSaveDraftRequest(
            @NotBlank(message = "Draft content is required")
            String content
    ) {
    }

    public record AgentMemoryUpdateRequest(
            @NotBlank(message = "Content is required")
            String content,
            @NotBlank(message = "Topic is required")
            String topicKey,
            @NotNull(message = "Memory scope is required")
            MemoryScope memoryScope,
            @NotNull(message = "Memory type is required")
            MemoryType memoryType,
            @NotNull(message = "Confidence score is required")
            BigDecimal confidenceScore,
            String sourceType,
            Long sourceRefId
    ) {
    }

    public record AgentMemoryDeleteRequest(
            boolean hardDelete
    ) {
    }

    public record AgentMemoryPinRequest(
            boolean pinned
    ) {
    }

    public record AgentToolRunRequest(
            @NotBlank(message = "Tool name is required")
            String toolName,
            String argumentsJson
    ) {
    }

    public record AgentTaskResponse(
            Long id,
            Long userId,
            String sessionId,
            String title,
            String goal,
            String taskType,
            String status,
            String executionMode,
            String searchScope,
            boolean allowDraftWrite,
            Integer currentStep,
            String finalOutputSummary,
            String errorMessage,
            LocalDateTime startedAt,
            LocalDateTime completedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record AgentTaskStepResponse(
            Long id,
            Integer stepIndex,
            String agentRole,
            String stepName,
            String status,
            String inputSummary,
            String outputSummary,
            Integer retryCount,
            Long latencyMs,
            LocalDateTime startedAt,
            LocalDateTime completedAt,
            LocalDateTime createdAt
    ) {
    }

    public record AgentTaskEventResponse(
            Long id,
            Long taskId,
            Long stepId,
            String eventType,
            String agentRole,
            String payloadSummary,
            String status,
            Long latencyMs,
            LocalDateTime createdAt
    ) {
    }

    public record AgentToolCallResponse(
            Long id,
            Long taskId,
            Long stepId,
            String toolName,
            String permissionLevel,
            String requestJson,
            String responseSummary,
            boolean success,
            String errorMessage,
            Long latencyMs,
            LocalDateTime createdAt
    ) {
    }

    public record AgentMemoryResponse(
            Long id,
            Long userId,
            String memoryScope,
            String topicKey,
            String memoryType,
            String content,
            String contentSummary,
            BigDecimal confidenceScore,
            String sourceType,
            Long sourceRefId,
            boolean pinned,
            boolean deleted,
            LocalDateTime lastHitAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record AgentMemoryHitResponse(
            Long id,
            Long taskId,
            Long memoryId,
            String topicKey,
            String hitReason,
            String usedInStep,
            LocalDateTime createdAt
    ) {
    }

    public record AgentEvalSummaryResponse(
            long total,
            double averageOverall,
            double averageGrounding,
            double averageHelpfulness,
            double averageStyle,
            long completedWithEvaluations
    ) {
    }

    public record AgentTaskListResponse(
            Long id,
            String title,
            String goal,
            String status,
            String taskType,
            String executionMode,
            String searchScope,
            Integer currentStep,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record AgentTaskDetailResponse(
            AgentTaskResponse task,
            List<AgentTaskStepResponse> steps,
            List<AgentTaskEventResponse> events,
            List<AgentToolCallResponse> toolCalls,
            List<AgentMemoryResponse> memories
    ) {
    }

    public record AgentTaskTraceResponse(
            AgentTaskResponse task,
            List<AgentTaskStepResponse> steps,
            List<AgentTaskEventResponse> events,
            PageResponse<AgentToolCallResponse> toolCalls,
            PageResponse<AgentMemoryHitResponse> memoryHits
    ) {
    }

    public record AgentTraceItem(
            AgentTaskEventResponse event,
            AgentToolCallResponse toolCall,
            Long taskId
    ) {
    }

    public record AgentToolResponse(
            boolean success,
            String summary,
            String payload
    ) {
    }

    public record ToolCatalogResponse(
            List<String> tools
    ) {
    }

    public static AgentTaskResponse toTaskResponse(
            Long id,
            Long userId,
            String sessionId,
            String title,
            String goal,
            TaskType taskType,
            TaskStatus status,
            ExecutionMode executionMode,
            SearchScope searchScope,
            Boolean allowDraftWrite,
            Integer currentStep,
            String finalOutputSummary,
            String errorMessage,
            LocalDateTime startedAt,
            LocalDateTime completedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        return new AgentTaskResponse(
                id,
                userId,
                sessionId,
                title,
                goal,
                taskType == null ? null : taskType.name(),
                status == null ? null : status.name(),
                executionMode == null ? null : executionMode.name(),
                searchScope == null ? null : searchScope.name(),
                Boolean.TRUE.equals(allowDraftWrite),
                currentStep,
                finalOutputSummary,
                errorMessage,
                startedAt,
                completedAt,
                createdAt,
                updatedAt
        );
    }

    public static AgentTaskStepResponse toStepResponse(
            Long id,
            Integer stepIndex,
            AgentRole agentRole,
            String stepName,
            TaskStatus status,
            String inputSummary,
            String outputSummary,
            Integer retryCount,
            Long latencyMs,
            LocalDateTime startedAt,
            LocalDateTime completedAt,
            LocalDateTime createdAt
    ) {
        return new AgentTaskStepResponse(
                id,
                stepIndex,
                agentRole == null ? null : agentRole.name(),
                stepName,
                status == null ? null : status.name(),
                inputSummary,
                outputSummary,
                retryCount,
                latencyMs,
                startedAt,
                completedAt,
                createdAt
        );
    }

    public static AgentTaskEventResponse toEventResponse(
            Long id,
            Long taskId,
            Long stepId,
            EventType eventType,
            AgentRole agentRole,
            String payloadSummary,
            String status,
            Long latencyMs,
            LocalDateTime createdAt
    ) {
        return new AgentTaskEventResponse(
                id,
                taskId,
                stepId,
                eventType == null ? null : eventType.name(),
                agentRole == null ? null : agentRole.name(),
                payloadSummary,
                status,
                latencyMs,
                createdAt
        );
    }
}
