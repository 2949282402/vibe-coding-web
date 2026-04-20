package com.hejulian.blog.mapper;

import com.hejulian.blog.entity.Tag;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface TagMapper {

    List<Tag> selectAllOrderByName();

    Tag selectById(@Param("id") Long id);

    Tag selectBySlug(@Param("slug") String slug);

    List<Tag> selectByIds(@Param("ids") List<Long> ids);

    List<Tag> selectTagsByPostId(@Param("postId") Long postId);

    long countAll();

    long countById(@Param("id") Long id);

    long countBySlug(@Param("slug") String slug);

    long countBySlugAndIdNot(@Param("slug") String slug, @Param("id") Long id);

    int insert(Tag tag);

    int update(Tag tag);

    int deleteById(@Param("id") Long id);
}
