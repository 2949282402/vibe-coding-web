package com.hejulian.blog.agent.domain.tool;

public record AgentToolContext(
        Long taskId,
        Long stepId,
        Long userId,
        String sessionId,
        boolean allowDraftWrite,
        boolean admin
) {
}

