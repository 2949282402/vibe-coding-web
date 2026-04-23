package com.hejulian.blog.agent.infrastructure.tool;

import com.hejulian.blog.agent.domain.tool.AgentTool;
import com.hejulian.blog.exception.BusinessException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AgentToolRegistry {

    private final Map<String, AgentTool> toolsByName = new LinkedHashMap<>();

    public AgentToolRegistry(List<AgentTool> tools) {
        for (AgentTool tool : tools) {
            toolsByName.put(tool.name(), tool);
        }
    }

    public AgentTool get(String name) {
        AgentTool tool = toolsByName.get(name);
        if (tool == null) {
            throw new BusinessException("Tool not found: " + name);
        }
        return tool;
    }

    public List<String> names() {
        return List.copyOf(toolsByName.keySet());
    }
}

