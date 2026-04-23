package com.hejulian.blog.agent.application;

import com.hejulian.blog.agent.domain.enums.TaskStatus;
import com.hejulian.blog.agent.dto.AgentDtos;
import com.hejulian.blog.agent.entity.AgentMemory;
import com.hejulian.blog.agent.entity.AgentTask;
import com.hejulian.blog.agent.entity.AgentTaskEvent;
import com.hejulian.blog.agent.entity.AgentTaskStep;
import com.hejulian.blog.agent.entity.AgentToolCall;
import com.hejulian.blog.agent.mapper.AgentEvalRecordMapper;
import com.hejulian.blog.agent.mapper.AgentMemoryHitMapper;
import com.hejulian.blog.agent.mapper.AgentMemoryMapper;
import com.hejulian.blog.agent.mapper.AgentTaskEventMapper;
import com.hejulian.blog.agent.mapper.AgentTaskMapper;
import com.hejulian.blog.agent.mapper.AgentTaskStepMapper;
import com.hejulian.blog.agent.mapper.AgentToolCallMapper;
import com.hejulian.blog.common.PageResponse;
import com.hejulian.blog.exception.BusinessException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgentAdminService {

    private final AgentTaskMapper taskMapper;
    private final AgentTaskStepMapper stepMapper;
    private final AgentTaskEventMapper eventMapper;
    private final AgentToolCallMapper toolCallMapper;
    private final AgentMemoryHitMapper memoryHitMapper;
    private final AgentMemoryMapper memoryMapper;
    private final AgentEvalRecordMapper evalRecordMapper;

    public AgentAdminService(
            AgentTaskMapper taskMapper,
            AgentTaskStepMapper stepMapper,
            AgentTaskEventMapper eventMapper,
            AgentToolCallMapper toolCallMapper,
            AgentMemoryHitMapper memoryHitMapper,
            AgentMemoryMapper memoryMapper,
            AgentEvalRecordMapper evalRecordMapper
    ) {
        this.taskMapper = taskMapper;
        this.stepMapper = stepMapper;
        this.eventMapper = eventMapper;
        this.toolCallMapper = toolCallMapper;
        this.memoryHitMapper = memoryHitMapper;
        this.memoryMapper = memoryMapper;
        this.evalRecordMapper = evalRecordMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<AgentDtos.AgentTaskListResponse> listTasks(String status, String keyword, int page, int pageSize) {
        int normalizedPage = normalizePage(page);
        int normalizedPageSize = normalizePageSize(pageSize);
        String normalizedStatus = sanitizeStatus(status);
        String normalizedKeyword = sanitizeKeyword(keyword);

        long total = taskMapper.countAll(null, normalizedStatus, normalizedKeyword);
        List<AgentDtos.AgentTaskListResponse> records = taskMapper.selectAll(
                        null,
                        normalizedStatus,
                        normalizedKeyword,
                        normalizedPage * normalizedPageSize,
                        normalizedPageSize
                ).stream()
                .map(task -> new AgentDtos.AgentTaskListResponse(
                        task.getId(),
                        task.getTitle(),
                        task.getGoal(),
                        task.getStatus() == null ? null : task.getStatus().name(),
                        task.getTaskType() == null ? null : task.getTaskType().name(),
                        task.getExecutionMode() == null ? null : task.getExecutionMode().name(),
                        task.getSearchScope() == null ? null : task.getSearchScope().name(),
                        task.getCurrentStep(),
                        task.getCreatedAt(),
                        task.getUpdatedAt()
                ))
                .toList();

        return buildPageResponse(records, normalizedPage + 1, normalizedPageSize, total);
    }

    @Transactional(readOnly = true)
    public AgentDtos.AgentTaskTraceResponse taskTrace(
            Long taskId,
            int toolCallPage,
            int toolCallPageSize,
            int memoryHitPage,
            int memoryHitPageSize
    ) {
        AgentTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("Task not found");
        }
        return new AgentDtos.AgentTaskTraceResponse(
                toTaskResponse(task),
                stepMapper.selectByTaskId(taskId).stream().map(this::toStepResponse).toList(),
                eventMapper.selectByTaskId(taskId).stream().map(this::toEventResponse).toList(),
                listToolCallsByTask(taskId, toolCallPage, toolCallPageSize),
                listMemoryHitsByTask(taskId, memoryHitPage, memoryHitPageSize)
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<AgentDtos.AgentToolCallResponse> listToolCalls(Long taskId, int page, int pageSize) {
        int normalizedPage = normalizePage(page);
        int normalizedPageSize = normalizePageSize(pageSize);
        List<AgentDtos.AgentToolCallResponse> records = (taskId == null ? toolCallMapper.selectAllPaged(
                        normalizedPage * normalizedPageSize,
                        normalizedPageSize
                ) : toolCallMapper.selectByTaskIdPaged(
                        taskId,
                        normalizedPage * normalizedPageSize,
                        normalizedPageSize
                ))
                .stream()
                .map(this::toToolCallResponse)
                .toList();

        long total = taskId == null ? toolCallMapper.countAll() : toolCallMapper.countByTaskId(taskId);
        return buildPageResponse(records, normalizedPage + 1, normalizedPageSize, total);
    }

    private PageResponse<AgentDtos.AgentToolCallResponse> listToolCallsByTask(Long taskId, int page, int pageSize) {
        int normalizedPage = normalizePage(page);
        int normalizedPageSize = normalizePageSize(pageSize);
        List<AgentDtos.AgentToolCallResponse> records = toolCallMapper.selectByTaskIdPaged(
                        taskId,
                        normalizedPage * normalizedPageSize,
                        normalizedPageSize
                ).stream()
                .map(this::toToolCallResponse)
                .toList();
        long total = toolCallMapper.countByTaskId(taskId);
        return buildPageResponse(records, normalizedPage + 1, normalizedPageSize, total);
    }

    private PageResponse<AgentDtos.AgentMemoryHitResponse> listMemoryHitsByTask(Long taskId, int page, int pageSize) {
        int normalizedPage = normalizePage(page);
        int normalizedPageSize = normalizePageSize(pageSize);
        List<AgentDtos.AgentMemoryHitResponse> records = memoryHitMapper.selectByTaskIdPaged(
                        taskId,
                        normalizedPage * normalizedPageSize,
                        normalizedPageSize
                ).stream()
                .map(hit -> new AgentDtos.AgentMemoryHitResponse(
                        hit.getId(),
                        hit.getTaskId(),
                        hit.getMemoryId(),
                        toMemoryTopic(hit.getMemoryId()),
                        hit.getHitReason(),
                        hit.getUsedInStep(),
                        hit.getCreatedAt()
                ))
                .toList();
        long total = memoryHitMapper.countByTaskId(taskId);
        return buildPageResponse(records, normalizedPage + 1, normalizedPageSize, total);
    }

    @Transactional(readOnly = true)
    public AgentDtos.AgentEvalSummaryResponse evalSummary() {
        long total = evalRecordMapper.countAll();
        Double avgOverall = evalRecordMapper.averageOverall();
        Double avgGrounding = evalRecordMapper.averageGrounding();
        Double avgHelpfulness = evalRecordMapper.averageHelpfulness();
        Double avgStyle = evalRecordMapper.averageStyle();
        return new AgentDtos.AgentEvalSummaryResponse(
                total,
                avgOverall == null ? 0D : avgOverall,
                avgGrounding == null ? 0D : avgGrounding,
                avgHelpfulness == null ? 0D : avgHelpfulness,
                avgStyle == null ? 0D : avgStyle,
                total
        );
    }

    private AgentDtos.AgentTaskResponse toTaskResponse(AgentTask task) {
        return new AgentDtos.AgentTaskResponse(
                task.getId(),
                task.getUserId(),
                task.getSessionId(),
                task.getTitle(),
                task.getGoal(),
                task.getTaskType() == null ? null : task.getTaskType().name(),
                task.getStatus() == null ? null : task.getStatus().name(),
                task.getExecutionMode() == null ? null : task.getExecutionMode().name(),
                task.getSearchScope() == null ? null : task.getSearchScope().name(),
                Boolean.TRUE.equals(task.getAllowDraftWrite()),
                task.getCurrentStep(),
                task.getFinalOutputSummary(),
                task.getErrorMessage(),
                task.getStartedAt(),
                task.getCompletedAt(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }

    private AgentDtos.AgentTaskStepResponse toStepResponse(AgentTaskStep step) {
        return new AgentDtos.AgentTaskStepResponse(
                step.getId(),
                step.getStepIndex(),
                step.getAgentRole() == null ? null : step.getAgentRole().name(),
                step.getStepName(),
                step.getStatus() == null ? null : step.getStatus().name(),
                step.getInputSummary(),
                step.getOutputSummary(),
                step.getRetryCount(),
                step.getLatencyMs(),
                step.getStartedAt(),
                step.getCompletedAt(),
                step.getCreatedAt()
        );
    }

    private AgentDtos.AgentTaskEventResponse toEventResponse(AgentTaskEvent event) {
        return new AgentDtos.AgentTaskEventResponse(
                event.getId(),
                event.getTaskId(),
                event.getStepId(),
                event.getEventType() == null ? null : event.getEventType().name(),
                event.getAgentRole() == null ? null : event.getAgentRole().name(),
                event.getPayloadSummary(),
                event.getStatus(),
                event.getLatencyMs(),
                event.getCreatedAt()
        );
    }

    private AgentDtos.AgentToolCallResponse toToolCallResponse(AgentToolCall call) {
        return new AgentDtos.AgentToolCallResponse(
                call.getId(),
                call.getTaskId(),
                call.getStepId(),
                call.getToolName(),
                call.getPermissionLevel() == null ? null : call.getPermissionLevel().name(),
                call.getRequestJson(),
                call.getResponseSummary(),
                Boolean.TRUE.equals(call.getSuccess()),
                call.getErrorMessage(),
                call.getLatencyMs(),
                call.getCreatedAt()
        );
    }

    private String toMemoryTopic(Long memoryId) {
        AgentMemory memory = memoryMapper.selectById(memoryId);
        return memory == null ? null : memory.getTopicKey();
    }

    private int normalizePage(int page) {
        return Math.max(page, 1) - 1;
    }

    private int normalizePageSize(int pageSize) {
        return Math.min(Math.max(pageSize, 1), 100);
    }

    private String sanitizeKeyword(String keyword) {
        return keyword == null ? null : keyword.trim();
    }

    private String sanitizeStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return null;
        }
        String normalized = status.trim().toUpperCase();
        try {
            TaskStatus.valueOf(normalized);
            return normalized;
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Unsupported task status: " + status);
        }
    }

    private <T> PageResponse<T> buildPageResponse(List<T> records, int page, int pageSize, long total) {
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / pageSize);
        return new PageResponse<>(records, page, pageSize, total, totalPages, page < totalPages);
    }
}
