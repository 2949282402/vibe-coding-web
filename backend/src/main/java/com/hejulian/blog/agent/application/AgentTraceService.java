package com.hejulian.blog.agent.application;

import com.hejulian.blog.agent.domain.enums.AgentRole;
import com.hejulian.blog.agent.domain.enums.EventType;
import com.hejulian.blog.agent.entity.AgentTaskEvent;
import com.hejulian.blog.agent.mapper.AgentTaskEventMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AgentTraceService {

    private final AgentTaskEventMapper eventMapper;

    public AgentTraceService(AgentTaskEventMapper eventMapper) {
        this.eventMapper = eventMapper;
    }

    public void recordEvent(Long taskId, Long stepId, AgentRole role, EventType eventType, String payloadSummary, String status, Long latencyMs) {
        AgentTaskEvent event = new AgentTaskEvent();
        event.setTaskId(taskId);
        event.setStepId(stepId);
        event.setAgentRole(role);
        event.setEventType(eventType);
        event.setPayloadSummary(limit(payloadSummary, 160));
        event.setPayloadJson(null);
        event.setStatus(status);
        event.setLatencyMs(latencyMs);
        event.setCreatedAt(LocalDateTime.now());
        eventMapper.insert(event);
    }

    public List<AgentTaskEvent> listEvents(Long taskId) {
        return eventMapper.selectByTaskId(taskId);
    }

    private String limit(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength) + "...";
    }
}

