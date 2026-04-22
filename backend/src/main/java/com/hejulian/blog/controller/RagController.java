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
        return withRuntime(authenticatedUser, () -> ApiResponse.success(ragApplicationService.askQuestion(request)));
    }

    @GetMapping("/history")
    public ApiResponse<RagDtos.HistoryResponse> history(@RequestParam("sessionId") String sessionId) {
        return ApiResponse.success(ragApplicationService.getHistory(sessionId));
    }

    @GetMapping("/sessions")
    public ApiResponse<RagDtos.SessionListResponse> sessions(
            @RequestParam(name = "includeDeleted", defaultValue = "false") boolean includeDeleted
    ) {
        return ApiResponse.success(ragApplicationService.getSessions(includeDeleted));
    }

    @PatchMapping("/sessions/{sessionId}")
    public ApiResponse<RagDtos.SessionSummary> renameSession(
            @PathVariable String sessionId,
            @Valid @RequestBody RagDtos.SessionUpdateRequest request
    ) {
        return ApiResponse.success(ragApplicationService.renameSession(sessionId, request.title()));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ApiResponse<Void> deleteSession(@PathVariable String sessionId) {
        ragApplicationService.deleteSession(sessionId);
        return ApiResponse.success(null);
    }

    @PostMapping("/sessions/{sessionId}/restore")
    public ApiResponse<RagDtos.SessionSummary> restoreSession(@PathVariable String sessionId) {
        return ApiResponse.success(ragApplicationService.restoreSession(sessionId));
    }

    @DeleteMapping("/sessions/{sessionId}/purge")
    public ApiResponse<Void> purgeSession(@PathVariable String sessionId) {
        ragApplicationService.purgeSession(sessionId);
        return ApiResponse.success(null);
    }

    @PostMapping("/feedback")
    public ApiResponse<RagDtos.ChatMessage> submitFeedback(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody RagDtos.FeedbackRequest request
    ) {
        authService.getCurrentProfile(authenticatedUser);
        return ApiResponse.success(ragApplicationService.submitFeedback(request));
    }

    @PostMapping("/replay")
    public ApiResponse<RagDtos.AskResponse> replay(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody RagDtos.ReplayRequest request
    ) {
        return withRuntime(authenticatedUser, () -> ApiResponse.success(ragApplicationService.replayConversation(request)));
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
        RagRuntimeContextHolder.RagRuntimeOptions runtime = authService.requireRagRuntimeOptions(authenticatedUser);
        return ragApplicationService.streamQuestion(request, runtime);
    }

    private <T> T withRuntime(AuthenticatedUser authenticatedUser, java.util.function.Supplier<T> supplier) {
        RagRuntimeContextHolder.set(authService.requireRagRuntimeOptions(authenticatedUser));
        try {
            return supplier.get();
        } finally {
            RagRuntimeContextHolder.clear();
        }
    }
}
