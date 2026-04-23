package com.hejulian.blog.agent.controller;

import com.hejulian.blog.agent.application.AgentTaskApplicationService;
import com.hejulian.blog.agent.dto.AgentDtos;
import com.hejulian.blog.common.ApiResponse;
import com.hejulian.blog.common.PageResponse;
import com.hejulian.blog.security.AuthenticatedUser;
import com.hejulian.blog.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/agent/tasks")
@RequiredArgsConstructor
public class AgentTaskController {

    private final AgentTaskApplicationService taskService;
    private final AuthService authService;

    @PostMapping
    public ApiResponse<AgentDtos.AgentTaskResponse> createTask(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody AgentDtos.AgentTaskCreateRequest request
    ) {
        return ApiResponse.success(taskService.createTask(requireUserId(authenticatedUser), request));
    }

    @GetMapping
    public ApiResponse<PageResponse<AgentDtos.AgentTaskListResponse>> listTasks(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        return ApiResponse.success(taskService.listTasks(
                requireUserId(authenticatedUser),
                status,
                keyword,
                page,
                pageSize
        ));
    }

    @GetMapping("/{taskId}")
    public ApiResponse<AgentDtos.AgentTaskDetailResponse> taskDetail(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long taskId
    ) {
        return ApiResponse.success(taskService.getTaskDetail(requireUserId(authenticatedUser), taskId));
    }

    @PostMapping("/{taskId}/cancel")
    public ApiResponse<Void> cancelTask(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long taskId
    ) {
        taskService.cancelTask(requireUserId(authenticatedUser), taskId);
        return ApiResponse.success("Task cancelled", null);
    }

    @PostMapping("/{taskId}/retry")
    public ApiResponse<AgentDtos.AgentTaskResponse> retryTask(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long taskId,
            @RequestBody(required = false) AgentDtos.AgentTaskRetryRequest request
    ) {
        String reason = request == null ? null : request.reason();
        return ApiResponse.success(taskService.retryTask(requireUserId(authenticatedUser), taskId, reason));
    }

    @PostMapping("/{taskId}/save-draft")
    public ApiResponse<AgentDtos.AgentTaskResponse> saveDraft(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long taskId,
            @Valid @RequestBody AgentDtos.AgentTaskSaveDraftRequest request
    ) {
        return ApiResponse.success(taskService.saveDraft(requireUserId(authenticatedUser), taskId, request));
    }

    @GetMapping(value = "/{taskId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamTask(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long taskId
    ) {
        return taskService.streamTask(requireUserId(authenticatedUser), taskId);
    }

    private Long requireUserId(AuthenticatedUser authenticatedUser) {
        authService.getCurrentProfile(authenticatedUser);
        return authenticatedUser.getId();
    }
}
