package com.hejulian.blog.mapper;

import com.hejulian.blog.entity.RagChatMessage;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface RagChatMessageMapper {

    List<RagChatMessage> selectBySessionId(@Param("sessionId") String sessionId);

    int insert(RagChatMessage message);
}
