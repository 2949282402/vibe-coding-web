package com.hejulian.blog.agent.domain.tool;

public record ToolExecutionResult(
        boolean success,
        String summary,
        String payload
) {
}

