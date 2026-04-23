package com.hejulian.blog.agent.application;

import com.hejulian.blog.agent.domain.enums.MemoryScope;
import com.hejulian.blog.agent.dto.AgentDtos;
import com.hejulian.blog.agent.entity.AgentMemory;
import com.hejulian.blog.agent.entity.AgentMemoryHit;
import com.hejulian.blog.agent.mapper.AgentMemoryHitMapper;
import com.hejulian.blog.agent.mapper.AgentMemoryMapper;
import com.hejulian.blog.exception.BusinessException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AgentMemoryService {

    private static final int MAX_SUMMARY_LENGTH = 140;
    private static final int MAX_TASK_MEMORIES = 8;

    private final AgentMemoryMapper memoryMapper;
    private final AgentMemoryHitMapper memoryHitMapper;

    public AgentMemoryService(AgentMemoryMapper memoryMapper, AgentMemoryHitMapper memoryHitMapper) {
        this.memoryMapper = memoryMapper;
        this.memoryHitMapper = memoryHitMapper;
    }

    @Transactional(readOnly = true)
    public List<AgentDtos.AgentMemoryResponse> listForUser(
            Long userId,
            String memoryScope,
            String keyword,
            Boolean onlyPinned,
            int page,
            int pageSize
    ) {
        int normalizedPage = normalizePage(page);
        int normalizedPageSize = normalizePageSize(pageSize);
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        List<AgentMemory> records = memoryMapper.selectByUserId(
                userId,
                normalizeScope(memoryScope),
                normalizedKeyword,
                onlyPinned,
                normalizedPage * normalizedPageSize,
                normalizedPageSize
        );
        return records.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public long countForUser(Long userId, String memoryScope, String keyword, Boolean onlyPinned) {
        return memoryMapper.countByUserId(userId, normalizeScope(memoryScope), sanitizeKeyword(keyword), onlyPinned);
    }

    @Transactional
    public void updatePin(Long userId, Long id, boolean pinned) {
        int updated = memoryMapper.updatePinned(id, userId, pinned);
        if (updated < 1) {
            throw new BusinessException("Memory not found");
        }
    }

    @Transactional
    public void delete(Long userId, Long id) {
        int updated = memoryMapper.markDeleted(id, userId);
        if (updated < 1) {
            throw new BusinessException("Memory not found");
        }
    }

    @Transactional
    public AgentDtos.AgentMemoryResponse createOrReplaceTaskMemory(Long userId, Long taskId, String content, String summary) {
        AgentMemory memory = new AgentMemory();
        memory.setUserId(userId);
        memory.setMemoryScope(MemoryScope.TASK);
        memory.setTopicKey("task_" + taskId);
        memory.setMemoryType(com.hejulian.blog.agent.domain.enums.MemoryType.NOTE);
        memory.setContent(content);
        memory.setContentSummary(summary);
        memory.setConfidenceScore(BigDecimal.valueOf(0.82));
        memory.setSourceType("agent_task");
        memory.setSourceRefId(taskId);
        memory.setPinned(Boolean.FALSE);
        memory.setDeleted(Boolean.FALSE);
        memory.setLastHitAt(LocalDateTime.now());
        memoryMapper.insert(memory);
        return toResponse(memory);
    }

    @Transactional
    public AgentDtos.AgentMemoryResponse upsertUserNote(Long userId, AgentDtos.AgentMemoryUpdateRequest request) {
        AgentMemory memory = new AgentMemory();
        memory.setUserId(userId);
        memory.setMemoryScope(request.memoryScope());
        memory.setTopicKey(sanitizeTopicKey(request.topicKey()));
        memory.setMemoryType(request.memoryType());
        memory.setContent(request.content());
        memory.setContentSummary(buildSummary(request.content()));
        memory.setConfidenceScore(request.confidenceScore());
        memory.setSourceType(request.sourceType());
        memory.setSourceRefId(request.sourceRefId());
        memory.setPinned(Boolean.FALSE);
        memory.setDeleted(Boolean.FALSE);
        memory.setLastHitAt(LocalDateTime.now());
        memoryMapper.insert(memory);
        return toResponse(memory);
    }

    @Transactional
    public List<AgentDtos.AgentMemoryResponse> recallForTask(Long userId, Long taskId, String queryHint) {
        String normalized = StringUtils.hasText(queryHint) ? queryHint.trim().toLowerCase(Locale.ROOT) : "";
        List<AgentMemory> candidates = memoryMapper.selectByUserId(userId, MemoryScope.USER.name(), null, null, 0, MAX_TASK_MEMORIES);
        List<AgentMemory> matched = new ArrayList<>();
        for (AgentMemory memory : candidates) {
            if (!Boolean.TRUE.equals(memory.getDeleted()) && containsKeyword(memory, normalized)) {
                matched.add(memory);
            }
        }
        for (AgentMemory memory : matched) {
            recordHit(taskId, memory.getId(), "keyword_match", "PLANNER");
            memoryMapper.touchHit(memory.getId(), LocalDateTime.now());
        }
        return matched.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<AgentDtos.AgentMemoryHitResponse> listHits(Long taskId) {
        return memoryHitMapper.selectByTaskId(taskId).stream()
                .map(hit -> new AgentDtos.AgentMemoryHitResponse(
                        hit.getId(),
                        hit.getTaskId(),
                        hit.getMemoryId(),
                        getMemoryTopic(hit.getMemoryId()),
                        hit.getHitReason(),
                        hit.getUsedInStep(),
                        hit.getCreatedAt()
                ))
                .toList();
    }

    @Transactional
    public void recordHit(Long taskId, Long memoryId, String hitReason, String stepName) {
        memoryHitMapper.insert(new AgentMemoryHit() {{
            setTaskId(taskId);
            setMemoryId(memoryId);
            setHitReason(hitReason);
            setUsedInStep(stepName);
            setCreatedAt(LocalDateTime.now());
        }});
    }

    public List<AgentMemory> listAllForTask(Long userId, Long taskId, int limit) {
        String normalizedTopic = "task_" + taskId;
        List<AgentMemory> memories = memoryMapper.selectByUserId(userId, MemoryScope.TASK.name(), null, null, 0, limit);
        return memories.stream().filter(memory -> normalizedTopic.equals(memory.getTopicKey())).toList();
    }

    private String buildSummary(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        String normalized = content.trim().replaceAll("\\s+", " ");
        return normalized.length() > MAX_SUMMARY_LENGTH ? normalized.substring(0, MAX_SUMMARY_LENGTH) + "..." : normalized;
    }

    private boolean containsKeyword(AgentMemory memory, String normalizedHint) {
        if (!StringUtils.hasText(normalizedHint)) {
            return false;
        }
        String content = StringUtils.hasText(memory.getContent()) ? memory.getContent().toLowerCase(Locale.ROOT) : "";
        String topic = StringUtils.hasText(memory.getTopicKey()) ? memory.getTopicKey().toLowerCase(Locale.ROOT) : "";
        return content.contains(normalizedHint) || topic.contains(normalizedHint);
    }

    private AgentDtos.AgentMemoryResponse toResponse(AgentMemory memory) {
        return new AgentDtos.AgentMemoryResponse(
                memory.getId(),
                memory.getUserId(),
                memory.getMemoryScope() == null ? null : memory.getMemoryScope().name(),
                memory.getTopicKey(),
                memory.getMemoryType() == null ? null : memory.getMemoryType().name(),
                memory.getContent(),
                memory.getContentSummary(),
                memory.getConfidenceScore(),
                memory.getSourceType(),
                memory.getSourceRefId(),
                Boolean.TRUE.equals(memory.getPinned()),
                Boolean.TRUE.equals(memory.getDeleted()),
                memory.getLastHitAt(),
                memory.getCreatedAt(),
                memory.getUpdatedAt()
        );
    }

    private String getMemoryTopic(Long memoryId) {
        AgentMemory memory = memoryMapper.selectById(memoryId);
        return memory == null ? null : memory.getTopicKey();
    }

    private String sanitizeTopicKey(String topicKey) {
        return topicKey.trim().replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    private String normalizeScope(String memoryScope) {
        return StringUtils.hasText(memoryScope) ? memoryScope.trim().toUpperCase(Locale.ROOT) : null;
    }

    private String sanitizeKeyword(String keyword) {
        return StringUtils.hasText(keyword) ? keyword.trim() : null;
    }

    private int normalizePage(int page) {
        return Math.max(page, 1) - 1;
    }

    private int normalizePageSize(int pageSize) {
        return Math.min(Math.max(pageSize, 1), 50);
    }
}

