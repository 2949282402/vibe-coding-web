package com.hejulian.blog.agent.controller;

import com.hejulian.blog.agent.application.AgentMemoryService;
import com.hejulian.blog.agent.dto.AgentDtos;
import com.hejulian.blog.common.ApiResponse;
import com.hejulian.blog.common.PageResponse;
import com.hejulian.blog.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent/memories")
@RequiredArgsConstructor
public class AgentMemoryController {

    private final AgentMemoryService memoryService;

    @GetMapping("/me")
    public ApiResponse<PageResponse<AgentDtos.AgentMemoryResponse>> me(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(required = false) String memoryScope,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "false") boolean pinned,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        Long userId = authenticatedUser.getId();
        Boolean pinnedFilter = pinned ? Boolean.TRUE : null;
        return ApiResponse.success(buildPageResponse(
                memoryService.listForUser(userId, memoryScope, keyword, pinnedFilter, page, pageSize),
                page,
                pageSize,
                memoryService.countForUser(userId, memoryScope, keyword, pinnedFilter)
        ));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteMemory(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long id
    ) {
        memoryService.delete(authenticatedUser.getId(), id);
        return ApiResponse.success("Memory deleted", null);
    }

    @PostMapping("/{id}/pin")
    public ApiResponse<Void> pinMemory(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long id,
            @Valid @RequestBody AgentDtos.AgentMemoryPinRequest request
    ) {
        memoryService.updatePin(authenticatedUser.getId(), id, request.pinned());
        return ApiResponse.success("Pin updated", null);
    }

    private <T> PageResponse<T> buildPageResponse(java.util.List<T> records, int page, int pageSize, long total) {
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.min(Math.max(pageSize, 1), 50);
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / normalizedSize);
        return new PageResponse<>(records, normalizedPage, normalizedSize, total, totalPages, normalizedPage < totalPages);
    }
}
