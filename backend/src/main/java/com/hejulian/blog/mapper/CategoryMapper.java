package com.hejulian.blog.mapper;

import com.hejulian.blog.entity.Category;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface CategoryMapper {

    List<Category> selectAllOrderByName();

    Category selectById(@Param("id") Long id);

    long countAll();

    long countById(@Param("id") Long id);

    long countBySlug(@Param("slug") String slug);

    long countBySlugAndIdNot(@Param("slug") String slug, @Param("id") Long id);

    int insert(Category category);

    int update(Category category);

    int deleteById(@Param("id") Long id);
}

