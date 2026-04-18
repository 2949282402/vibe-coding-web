package com.hejulian.blog.mapper;

import com.hejulian.blog.entity.Comment;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface CommentMapper {

    long countByStatus(@Param("status") String status);

    List<Comment> selectByPostIdAndStatusOrderByCreatedAtAsc(@Param("postId") Long postId, @Param("status") String status);

    List<Comment> selectAllOrderByCreatedAtDesc();

    List<Comment> selectRecentComments(@Param("limit") int limit);

    Comment selectById(@Param("id") Long id);

    int insert(Comment comment);

    int updateStatus(@Param("id") Long id, @Param("status") String status);

    int deleteByPostId(@Param("postId") Long postId);
}
