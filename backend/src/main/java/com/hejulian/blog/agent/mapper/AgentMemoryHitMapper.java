package com.hejulian.blog.agent.mapper;

import com.hejulian.blog.agent.entity.AgentMemoryHit;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface AgentMemoryHitMapper {

    int insert(AgentMemoryHit memoryHit);

    List<AgentMemoryHit> selectByTaskId(@Param("taskId") Long taskId);

    List<AgentMemoryHit> selectByTaskIdPaged(@Param("taskId") Long taskId, @Param("offset") int offset, @Param("limit") int limit);

    List<AgentMemoryHit> selectAllPaged(@Param("offset") int offset, @Param("limit") int limit);

    long countAll();

    long countByTaskId(@Param("taskId") Long taskId);
}
