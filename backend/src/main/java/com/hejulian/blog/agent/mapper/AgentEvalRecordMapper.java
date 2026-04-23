package com.hejulian.blog.agent.mapper;

import com.hejulian.blog.agent.entity.AgentEvalRecord;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface AgentEvalRecordMapper {

    int insert(AgentEvalRecord record);

    AgentEvalRecord selectByTaskId(@Param("taskId") Long taskId);

    long countAll();

    Double averageOverall();

    Double averageGrounding();

    Double averageHelpfulness();

    Double averageStyle();
}
