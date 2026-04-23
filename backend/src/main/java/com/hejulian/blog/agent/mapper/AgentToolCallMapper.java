package com.hejulian.blog.agent.mapper;

import com.hejulian.blog.agent.entity.AgentToolCall;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface AgentToolCallMapper {

    int insert(AgentToolCall toolCall);

    int update(@Param("id") Long id, @Param("success") Boolean success, @Param("responseSummary") String responseSummary, @Param("errorMessage") String errorMessage);

    List<AgentToolCall> selectByTaskId(@Param("taskId") Long taskId);

    List<AgentToolCall> selectByTaskIdPaged(@Param("taskId") Long taskId, @Param("offset") int offset, @Param("limit") int limit);

    List<AgentToolCall> selectAllPaged(@Param("offset") int offset, @Param("limit") int limit);

    List<AgentToolCall> selectByTaskIdForAdmin(@Param("taskId") Long taskId);

    long countByTaskId(@Param("taskId") Long taskId);

    long countAll();
}
