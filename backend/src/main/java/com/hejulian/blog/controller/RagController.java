package com.hejulian.blog.controller;

import com.hejulian.blog.common.ApiResponse;
import com.hejulian.blog.dto.AuthDtos;
import com.hejulian.blog.dto.RagDtos;
import com.hejulian.blog.rag.application.RagRuntimeContextHolder;
import com.hejulian.blog.rag.application.RagApplicationService;
import com.hejulian.blog.security.AuthenticatedUser;
import com.hejulian.blog.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/public/rag")
@RequiredArgsConstructor
public class RagController {

    private final RagApplicationService ragApplicationService;
    private final AuthService authService;

    @PostMapping("/ask")
    public ApiResponse<RagDtos.AskResponse> ask(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody RagDtos.AskRequest request
    ) {
        Long userId = requireUserId(authenticatedUser);
        return withRuntime(authenticatedUser, () -> ApiResponse.success(ragApplicationService.askQuestion(userId, request)));
    }

    @GetMapping("/history")
    public ApiResponse<RagDtos.HistoryResponse> history(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam("sessionId") String sessionId
    ) {
        return ApiResponse.success(ragApplicationService.getHistory(requireUserId(authenticatedUser), sessionId));
    }

    @GetMapping("/sessions")
    public ApiResponse<RagDtos.SessionListResponse> sessions(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(name = "includeDeleted", defaultValue = "false") boolean includeDeleted
    ) {
        return ApiResponse.success(ragApplicationService.getSessions(requireUserId(authenticatedUser), includeDeleted));
    }

    @PatchMapping("/sessions/{sessionId}")
    public ApiResponse<RagDtos.SessionSummary> renameSession(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable String sessionId,
            @Valid @RequestBody RagDtos.SessionUpdateRequest request
    ) {
        return ApiResponse.success(ragApplicationService.renameSession(requireUserId(authenticatedUser), sessionId, request.title()));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ApiResponse<Void> deleteSession(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable String sessionId
    ) {
        ragApplicationService.deleteSession(requireUserId(authenticatedUser), sessionId);
        return ApiResponse.success(null);
    }

    @PostMapping("/sessions/{sessionId}/restore")
    public ApiResponse<RagDtos.SessionSummary> restoreSession(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable String sessionId
    ) {
        return ApiResponse.success(ragApplicationService.restoreSession(requireUserId(authenticatedUser), sessionId));
    }

    @DeleteMapping("/sessions/{sessionId}/purge")
    public ApiResponse<Void> purgeSession(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable String sessionId
    ) {
        ragApplicationService.purgeSession(requireUserId(authenticatedUser), sessionId);
        return ApiResponse.success(null);
    }

    @PostMapping("/feedback")
    public ApiResponse<RagDtos.ChatMessage> submitFeedback(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody RagDtos.FeedbackRequest request
    ) {
        return ApiResponse.success(ragApplicationService.submitFeedback(requireUserId(authenticatedUser), request));
    }

    @PostMapping("/replay")
    public ApiResponse<RagDtos.AskResponse> replay(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody RagDtos.ReplayRequest request
    ) {
        Long userId = requireUserId(authenticatedUser);
        return withRuntime(authenticatedUser, () -> ApiResponse.success(ragApplicationService.replayConversation(userId, request)));
    }

    @GetMapping("/search")
    public ApiResponse<RagDtos.SearchResponse> search(@RequestParam("question") String question) {
        return ApiResponse.success(ragApplicationService.searchOnly(question));
    }

    @PostMapping(value = "/ask/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter askStream(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody RagDtos.AskRequest request
    ) {
        Long userId = requireUserId(authenticatedUser);
        RagRuntimeContextHolder.RagRuntimeOptions runtime = authService.requireRagRuntimeOptions(authenticatedUser);
        return ragApplicationService.streamQuestion(userId, request, runtime);
    }

    private <T> T withRuntime(AuthenticatedUser authenticatedUser, java.util.function.Supplier<T> supplier) {
        RagRuntimeContextHolder.set(authService.requireRagRuntimeOptions(authenticatedUser));
        try {
            return supplier.get();
        } finally {
            RagRuntimeContextHolder.clear();
        }
    }

    private Long requireUserId(AuthenticatedUser authenticatedUser) {
        authService.getCurrentProfile(authenticatedUser);
        return authenticatedUser.getId();
    }
}
