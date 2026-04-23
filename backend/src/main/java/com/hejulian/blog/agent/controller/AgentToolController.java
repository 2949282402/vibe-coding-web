package com.hejulian.blog.agent.controller;

import com.hejulian.blog.agent.application.AgentTaskApplicationService;
import com.hejulian.blog.agent.dto.AgentDtos;
import com.hejulian.blog.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent/tools")
@RequiredArgsConstructor
public class AgentToolController {

    private final AgentTaskApplicationService taskService;

    @GetMapping
    public ApiResponse<AgentDtos.ToolCatalogResponse> toolCatalog() {
        return ApiResponse.success(taskService.listTools());
    }
}
