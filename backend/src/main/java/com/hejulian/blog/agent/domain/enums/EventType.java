package com.hejulian.blog.agent.domain.enums;

public enum EventType {
    TASK_CREATED,
    STEP_STARTED,
    STEP_COMPLETED,
    STEP_FAILED,
    TOOL_CALL,
    TASK_COMPLETED,
    TASK_CANCELLED,
    TASK_RETRY,
    TASK_FAILED,
    MEMORY_HIT,
    TOOL_FAIL
}

