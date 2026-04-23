package com.hejulian.blog.agent.controller.admin;

import com.hejulian.blog.agent.application.AgentAdminService;
import com.hejulian.blog.agent.dto.AgentDtos;
import com.hejulian.blog.common.ApiResponse;
import com.hejulian.blog.common.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/agent")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminAgentController {

    private final AgentAdminService agentAdminService;

    @GetMapping("/tasks")
    public ApiResponse<PageResponse<AgentDtos.AgentTaskListResponse>> listTasks(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        return ApiResponse.success(agentAdminService.listTasks(status, keyword, page, pageSize));
    }

    @GetMapping("/tasks/{taskId}/trace")
    public ApiResponse<AgentDtos.AgentTaskTraceResponse> taskTrace(
            @PathVariable Long taskId,
            @RequestParam(defaultValue = "1") int toolCallPage,
            @RequestParam(defaultValue = "10") int toolCallPageSize,
            @RequestParam(defaultValue = "1") int memoryHitPage,
            @RequestParam(defaultValue = "10") int memoryHitPageSize
    ) {
        return ApiResponse.success(agentAdminService.taskTrace(
                taskId,
                toolCallPage,
                toolCallPageSize,
                memoryHitPage,
                memoryHitPageSize
        ));
    }

    @GetMapping("/tool-calls")
    public ApiResponse<PageResponse<AgentDtos.AgentToolCallResponse>> toolCalls(
            @RequestParam(required = false) Long taskId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        return ApiResponse.success(agentAdminService.listToolCalls(taskId, page, pageSize));
    }

    @GetMapping("/evals/summary")
    public ApiResponse<AgentDtos.AgentEvalSummaryResponse> evalSummary() {
        return ApiResponse.success(agentAdminService.evalSummary());
    }
}
