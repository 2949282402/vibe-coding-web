package com.hejulian.blog.mapper;

import com.hejulian.blog.entity.Post;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface PostMapper {

    long countAll();

    long countByStatus(@Param("status") String status);

    long countByCategoryId(@Param("categoryId") Long categoryId);

    long countByCategoryIdAndStatus(@Param("categoryId") Long categoryId, @Param("status") String status);

    long countByTagId(@Param("tagId") Long tagId);

    long countByTagIdAndStatus(@Param("tagId") Long tagId, @Param("status") String status);

    long countBySlug(@Param("slug") String slug);

    long countBySlugAndIdNot(@Param("slug") String slug, @Param("id") Long id);

    long countPublicPosts(@Param("keyword") String keyword,
                          @Param("categorySlug") String categorySlug,
                          @Param("tagSlug") String tagSlug);

    List<Post> selectPublicPosts(@Param("keyword") String keyword,
                                 @Param("categorySlug") String categorySlug,
                                 @Param("tagSlug") String tagSlug,
                                 @Param("offset") int offset,
                                 @Param("limit") int limit);

    List<Post> selectTopPublished(@Param("status") String status, @Param("limit") int limit);

    List<Post> selectTopFeaturedPublished(@Param("status") String status, @Param("limit") int limit);

    List<Post> selectAllPublishedForRag(@Param("status") String status);

    Post selectById(@Param("id") Long id);

    Post selectBySlugAndStatus(@Param("slug") String slug, @Param("status") String status);

    long countAdminPosts(@Param("keyword") String keyword,
                         @Param("status") String status,
                         @Param("categoryId") Long categoryId);

    List<Post> selectAdminPosts(@Param("keyword") String keyword,
                                @Param("status") String status,
                                @Param("categoryId") Long categoryId,
                                @Param("offset") int offset,
                                @Param("limit") int limit);

    List<Post> selectRecentUpdated(@Param("limit") int limit);

    int insert(Post post);

    int update(Post post);

    int deleteById(@Param("id") Long id);

    int incrementViewCount(@Param("id") Long id);

    int deletePostTags(@Param("postId") Long postId);

    int insertPostTags(@Param("postId") Long postId, @Param("tagIds") List<Long> tagIds);
}
