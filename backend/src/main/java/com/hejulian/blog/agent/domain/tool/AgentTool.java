package com.hejulian.blog.agent.domain.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.hejulian.blog.agent.domain.enums.ToolPermissionLevel;

public interface AgentTool {

    String name();

    String description();

    JsonNode inputSchema();

    ToolPermissionLevel permissionLevel();

    void validateInput(JsonNode args);

    ToolExecutionResult execute(AgentToolContext context, JsonNode args);
}

