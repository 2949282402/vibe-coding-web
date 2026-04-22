package com.hejulian.blog.mapper;

import com.hejulian.blog.entity.RagChatMessage;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface RagChatMessageMapper {

    long countAssistantMessages();

    long countAssistantMessagesWithFeedback();

    long countAssistantMessagesByFeedback(@Param("helpful") boolean helpful);

    List<RagChatMessage> selectRecentFeedback(@Param("limit") int limit);

    long countFilteredFeedback(
            @Param("keyword") String keyword,
            @Param("helpful") Boolean helpful,
            @Param("feedbackFrom") LocalDateTime feedbackFrom,
            @Param("feedbackTo") LocalDateTime feedbackTo
    );

    List<RagChatMessage> selectFilteredFeedback(
            @Param("keyword") String keyword,
            @Param("helpful") Boolean helpful,
            @Param("feedbackFrom") LocalDateTime feedbackFrom,
            @Param("feedbackTo") LocalDateTime feedbackTo,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    List<RagChatMessage> selectAllFilteredFeedback(
            @Param("keyword") String keyword,
            @Param("helpful") Boolean helpful,
            @Param("feedbackFrom") LocalDateTime feedbackFrom,
            @Param("feedbackTo") LocalDateTime feedbackTo
    );

    List<RagChatMessage> selectBySessionId(@Param("sessionId") String sessionId);

    int insert(RagChatMessage message);

    int updateFeedback(@Param("messageId") Long messageId, @Param("helpful") Boolean helpful, @Param("note") String note);

    int updateVariants(@Param("messageId") Long messageId, @Param("variantsJson") String variantsJson);

    int deleteBySessionIdFromMessageId(@Param("sessionId") String sessionId, @Param("fromMessageId") Long fromMessageId);

    int deleteBySessionId(@Param("sessionId") String sessionId);
}
