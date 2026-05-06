package com.hejulian.blog.agent.application;

import com.hejulian.blog.agent.domain.enums.ExecutionMode;
import com.hejulian.blog.agent.domain.enums.SearchScope;
import com.hejulian.blog.agent.domain.enums.TaskStatus;
import com.hejulian.blog.agent.domain.enums.TaskType;
import com.hejulian.blog.agent.dto.AgentDtos;
import com.hejulian.blog.agent.entity.AgentMemory;
import com.hejulian.blog.agent.entity.AgentTask;
import com.hejulian.blog.agent.mapper.AgentMemoryMapper;
import com.hejulian.blog.agent.mapper.AgentTaskEventMapper;
import com.hejulian.blog.agent.mapper.AgentTaskMapper;
import com.hejulian.blog.agent.mapper.AgentTaskStepMapper;
import com.hejulian.blog.agent.mapper.AgentToolCallMapper;
import com.hejulian.blog.common.PageResponse;
import com.hejulian.blog.exception.BusinessException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class AgentTaskApplicationService {

    private static final String REVIEW_STATUS_DRAFT_READY = "DRAFT_READY";

    private final AgentTaskMapper taskMapper;
    private final AgentTaskStepMapper stepMapper;
    private final AgentTaskEventMapper eventMapper;
    private final AgentToolCallMapper toolCallMapper;
    private final AgentMemoryMapper memoryMapper;
    private final AgentOrchestratorService orchestratorService;
    private final AgentMemoryService memoryService;
    private final AgentToolService toolService;

    public AgentTaskApplicationService(
            AgentTaskMapper taskMapper,
            AgentTaskStepMapper stepMapper,
            AgentTaskEventMapper eventMapper,
            AgentToolCallMapper toolCallMapper,
            AgentMemoryMapper memoryMapper,
            AgentOrchestratorService orchestratorService,
            AgentMemoryService memoryService,
            AgentToolService toolService
    ) {
        this.taskMapper = taskMapper;
        this.stepMapper = stepMapper;
        this.eventMapper = eventMapper;
        this.toolCallMapper = toolCallMapper;
        this.memoryMapper = memoryMapper;
        this.orchestratorService = orchestratorService;
        this.memoryService = memoryService;
        this.toolService = toolService;
    }

    public AgentDtos.AgentTaskResponse createTask(Long userId, AgentDtos.AgentTaskCreateRequest request) {
        AgentTask task = new AgentTask();
        task.setUserId(userId);
        task.setSessionId(resolveSessionId(request.sessionId(), request.title()));
        task.setTaskType(request.taskType() == null ? TaskType.BLOG_DRAFT : request.taskType());
        task.setTitle(normalizeTitle(request.title(), request.goal()));
        task.setGoal(request.goal().trim());
        task.setStatus(TaskStatus.PENDING);
        task.setExecutionMode(request.executionMode() == null ? ExecutionMode.AUTO : request.executionMode());
        task.setSearchScope(request.searchScope() == null ? SearchScope.LOCAL_ONLY : request.searchScope());
        task.setAllowDraftWrite(Boolean.TRUE.equals(request.allowDraftWrite()));
        task.setCurrentStep(0);
        task.setReviewStatus(null);
        task.setDraftPostId(null);
        task.setReviewedBy(null);
        task.setReviewedAt(null);
        task.setRejectReason(null);
        taskMapper.insert(task);

        Long taskId = task.getId();
        CompletableFuture.runAsync(() -> orchestratorService.runTask(taskId));
        return toTaskResponse(task);
    }

    @Transactional(readOnly = true)
    public PageResponse<AgentDtos.AgentTaskListResponse> listTasks(Long userId, String status, String keyword, int page, int pageSize) {
        int normalizedPage = normalizePage(page);
        int normalizedPageSize = normalizePageSize(pageSize);
        String normalizedStatus = normalizeStatus(status);
        String normalizedKeyword = sanitize(keyword);

        long total = taskMapper.countAll(userId, normalizedStatus, normalizedKeyword);
        List<AgentDtos.AgentTaskListResponse> records = taskMapper.selectAll(userId, normalizedStatus, normalizedKeyword,
                        normalizedPage * normalizedPageSize, normalizedPageSize)
                .stream()
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
    public AgentDtos.AgentTaskDetailResponse getTaskDetail(Long userId, Long taskId) {
        AgentTask task = taskMapper.selectByIdAndUserId(taskId, userId);
        if (task == null) {
            throw new BusinessException("Task not found");
        }
        return buildTaskDetail(task);
    }

    @Transactional
    public void cancelTask(Long userId, Long taskId) {
        AgentTask task = taskMapper.selectByIdAndUserId(taskId, userId);
        if (task == null) {
            throw new BusinessException("Task not found");
        }
        if (TaskStatus.COMPLETED.equals(task.getStatus()) || TaskStatus.CANCELLED.equals(task.getStatus())) {
            return;
        }
        taskMapper.updateStatus(taskId, TaskStatus.CANCELLED, task.getCurrentStep(),
                task.getFinalOutputSummary(),
                "User cancelled",
                task.getStartedAt(),
                LocalDateTime.now());
    }

    public AgentDtos.AgentTaskResponse retryTask(Long userId, Long taskId, String reason) {
        AgentTask task = taskMapper.selectByIdAndUserId(taskId, userId);
        if (task == null) {
            throw new BusinessException("Task not found");
        }
        taskMapper.updateStatus(taskId, TaskStatus.PENDING, task.getCurrentStep(), "", "", LocalDateTime.now(), null);
        taskMapper.updateReviewState(taskId, null, null, null, null, null);
        CompletableFuture.runAsync(() -> orchestratorService.retryTask(taskId, reason));
        AgentTask queued = taskMapper.selectById(taskId);
        return toTaskResponse(queued == null ? task : queued);
    }

    @Transactional(readOnly = true)
    public SseEmitter streamTask(Long userId, Long taskId) {
        AgentTask task = taskMapper.selectByIdAndUserId(taskId, userId);
        if (task == null) {
            throw new BusinessException("Task not found");
        }
        SseEmitter emitter = new SseEmitter(300000L);
        CompletableFuture.runAsync(() -> streamTaskSnapshots(emitter, userId, taskId));
        return emitter;
    }

    @Transactional
    public AgentDtos.AgentTaskResponse saveDraft(Long userId, Long taskId, AgentDtos.AgentTaskSaveDraftRequest request) {
        AgentTask task = taskMapper.selectByIdAndUserId(taskId, userId);
        if (task == null) {
            throw new BusinessException("Task not found");
        }
        if (!Boolean.TRUE.equals(task.getAllowDraftWrite())) {
            throw new BusinessException("Task does not allow draft write");
        }
        String content = request.content().trim();

        memoryMapper.insert(new AgentMemory() {{
            setUserId(userId);
            setMemoryScope(com.hejulian.blog.agent.domain.enums.MemoryScope.TASK);
            setTopicKey("task_" + taskId);
            setMemoryType(com.hejulian.blog.agent.domain.enums.MemoryType.NOTE);
            setContent(content);
            setContentSummary(content.length() > 180 ? content.substring(0, 180) + "..." : content);
            setConfidenceScore(null);
            setSourceType("manual_save_draft");
            setSourceRefId(taskId);
            setPinned(Boolean.FALSE);
            setDeleted(Boolean.FALSE);
            setLastHitAt(LocalDateTime.now());
        }});

        taskMapper.updateStatus(taskId, null, task.getCurrentStep(), content, task.getErrorMessage(),
                task.getStartedAt(), task.getCompletedAt());
        if (TaskStatus.COMPLETED.equals(task.getStatus()) && task.getDraftPostId() != null) {
            taskMapper.updateReviewState(taskId, REVIEW_STATUS_DRAFT_READY, task.getDraftPostId(), null, null, null);
        }
        task = taskMapper.selectById(taskId);
        return toTaskResponse(task);
    }

    @Transactional(readOnly = true)
    public AgentDtos.ToolCatalogResponse listTools() {
        return new AgentDtos.ToolCatalogResponse(toolService.listTools());
    }

    private AgentDtos.AgentTaskDetailResponse buildTaskDetail(AgentTask task) {
        return new AgentDtos.AgentTaskDetailResponse(
                toTaskResponse(task),
                stepMapper.selectByTaskId(task.getId()).stream()
                        .map(step -> new AgentDtos.AgentTaskStepResponse(
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
                        ))
                        .toList(),
                eventMapper.selectByTaskId(task.getId()).stream()
                        .map(event -> new AgentDtos.AgentTaskEventResponse(
                                event.getId(),
                                event.getTaskId(),
                                event.getStepId(),
                                event.getEventType() == null ? null : event.getEventType().name(),
                                event.getAgentRole() == null ? null : event.getAgentRole().name(),
                                event.getPayloadSummary(),
                                event.getStatus(),
                                event.getLatencyMs(),
                                event.getCreatedAt()
                        ))
                        .toList(),
                toolCallMapper.selectByTaskId(task.getId()).stream()
                        .map(call -> new AgentDtos.AgentToolCallResponse(
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
                        ))
                        .toList(),
                memoryService.listAllForTask(task.getUserId(), task.getId(), 100).stream()
                        .map(this::toMemoryResponse)
                        .toList()
        );
    }

    private AgentDtos.AgentMemoryResponse toMemoryResponse(AgentMemory memory) {
        return new AgentDtos.AgentMemoryResponse(
                memory.getId(),
                memory.getUserId(),
                memory.getMemoryScope() == null ? null : memory.getMemoryScope().name(),
                memory.getTopicKey(),
                memory.getMemoryType() == null ? null : memory.getMemoryType().name(),
                memory.getContent(),
                memory.getContentSummary(),
                memory.getConfidenceScore(),
                memory.getSourceType(),
                memory.getSourceRefId(),
                Boolean.TRUE.equals(memory.getPinned()),
                Boolean.TRUE.equals(memory.getDeleted()),
                memory.getLastHitAt(),
                memory.getCreatedAt(),
                memory.getUpdatedAt()
        );
    }

    private void streamTaskSnapshots(SseEmitter emitter, Long userId, Long taskId) {
        try {
            String lastProgressMessage = "";
            for (int index = 0; index < 300; index++) {
                AgentTask task = taskMapper.selectByIdAndUserId(taskId, userId);
                if (task == null) {
                    emitter.send(SseEmitter.event().name("error").data("Task not found"));
                    emitter.complete();
                    return;
                }

                AgentDtos.AgentTaskDetailResponse detail = buildTaskDetail(task);
                String progressMessage = buildProgressMessage(task);
                if (!progressMessage.equals(lastProgressMessage) || index % 2 == 0) {
                    emitter.send(SseEmitter.event().name("progress").data(Map.of(
                            "taskId", taskId,
                            "status", task.getStatus() == null ? "UNKNOWN" : task.getStatus().name(),
                            "currentStep", task.getCurrentStep() == null ? 0 : task.getCurrentStep(),
                            "message", progressMessage,
                            "createdAt", LocalDateTime.now().toString()
                    )));
                    lastProgressMessage = progressMessage;
                }
                emitter.send(SseEmitter.event().name("snapshot").data(detail));
                if (isTerminal(task.getStatus())) {
                    emitter.send(SseEmitter.event().name("done").data(detail));
                    emitter.complete();
                    return;
                }
                Thread.sleep(1000L);
            }
            emitter.send(SseEmitter.event().name("timeout").data("Task stream timeout"));
            emitter.complete();
        } catch (Exception ex) {
            emitter.completeWithError(ex);
        }
    }

    private String buildProgressMessage(AgentTask task) {
        TaskStatus status = task.getStatus();
        if (TaskStatus.PENDING.equals(status)) {
            return "Agent task queued. Preparing the workflow...";
        }
        if (TaskStatus.COMPLETED.equals(status)) {
            if (REVIEW_STATUS_DRAFT_READY.equals(task.getReviewStatus())) {
                return "Agent task completed. Draft is ready for admin review.";
            }
            return "Agent task completed. Preparing the final answer...";
        }
        if (TaskStatus.FAILED.equals(status)) {
            return "Agent task failed. Returning the error details...";
        }
        if (TaskStatus.CANCELLED.equals(status)) {
            return "Agent task cancelled.";
        }

        int step = task.getCurrentStep() == null ? 0 : task.getCurrentStep();
        return switch (step) {
            case 1 -> "Planner agent is reading the request and building a plan...";
            case 2 -> switch (task.getSearchScope() == null ? SearchScope.LOCAL_ONLY : task.getSearchScope()) {
                case WEB_ONLY -> "Research agent is searching web sources...";
                case LOCAL_AND_WEB -> "Research agent is searching site content and web sources...";
                default -> "Research agent is searching site content...";
            };
            case 3 -> "Writer agent is composing the article draft...";
            case 4 -> "Reviewer agent is checking the draft quality...";
            case 5 -> "Publisher agent is saving the article as a review draft...";
            default -> "Agent is starting up and coordinating the next step...";
        };
    }

    private boolean isTerminal(TaskStatus status) {
        return TaskStatus.COMPLETED.equals(status)
                || TaskStatus.FAILED.equals(status)
                || TaskStatus.CANCELLED.equals(status);
    }

    private AgentDtos.AgentTaskResponse toTaskResponse(AgentTask task) {
        return AgentDtos.toTaskResponse(
                task.getId(),
                task.getUserId(),
                task.getSessionId(),
                task.getTitle(),
                task.getGoal(),
                task.getTaskType(),
                task.getStatus(),
                task.getExecutionMode(),
                task.getSearchScope(),
                task.getAllowDraftWrite(),
                task.getCurrentStep(),
                task.getFinalOutputSummary(),
                task.getReviewStatus(),
                task.getDraftPostId(),
                task.getReviewedBy(),
                task.getReviewedAt(),
                task.getRejectReason(),
                task.getErrorMessage(),
                task.getStartedAt(),
                task.getCompletedAt(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }

    private int normalizePage(int page) {
        return Math.max(page, 1) - 1;
    }

    private int normalizePageSize(int pageSize) {
        return Math.min(Math.max(pageSize, 1), 50);
    }

    private String sanitize(String keyword) {
        return StringUtils.hasText(keyword) ? keyword.trim() : null;
    }

    private String normalizeStatus(String status) {
        return StringUtils.hasText(status) ? status.trim().toUpperCase() : null;
    }

    private String resolveSessionId(String sessionId, String title) {
        if (StringUtils.hasText(sessionId)) {
            return sessionId.trim();
        }
        if (!StringUtils.hasText(title)) {
            return UUID.randomUUID().toString();
        }
        return title.trim();
    }

    private String normalizeTitle(String title, String goal) {
        String normalized = StringUtils.hasText(title) ? title.trim() : null;
        if (StringUtils.hasText(normalized)) {
            return normalized;
        }
        if (StringUtils.hasText(goal)) {
            return goal.trim().substring(0, Math.min(goal.length(), 40));
        }
        return "Agent Task";
    }

    private <T> PageResponse<T> buildPageResponse(List<T> records, int page, int pageSize, long total) {
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / pageSize);
        boolean hasNext = page < totalPages;
        return new PageResponse<>(records, page, pageSize, total, totalPages, hasNext);
    }
}
