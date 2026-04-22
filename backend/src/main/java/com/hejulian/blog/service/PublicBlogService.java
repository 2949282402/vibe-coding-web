package com.hejulian.blog.service;

import com.hejulian.blog.common.CacheNames;
import com.hejulian.blog.common.PageResponse;
import com.hejulian.blog.dto.BlogDtos;
import com.hejulian.blog.entity.Comment;
import com.hejulian.blog.entity.CommentStatus;
import com.hejulian.blog.entity.Post;
import com.hejulian.blog.entity.PostStatus;
import com.hejulian.blog.entity.UserAccount;
import com.hejulian.blog.exception.BusinessException;
import com.hejulian.blog.mapper.CategoryMapper;
import com.hejulian.blog.mapper.CommentMapper;
import com.hejulian.blog.mapper.PostMapper;
import com.hejulian.blog.mapper.TagMapper;
import com.hejulian.blog.mapper.UserAccountMapper;
import com.hejulian.blog.security.AuthenticatedUser;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PublicBlogService {

    private final PostMapper postMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final CommentMapper commentMapper;
    private final UserAccountMapper userAccountMapper;
    private final CacheManager cacheManager;

    @Value("${blog.site.name}")
    private String siteName;

    @Value("${blog.site.hero-title}")
    private String heroTitle;

    @Value("${blog.site.hero-subtitle}")
    private String heroSubtitle;

    @Cacheable(CacheNames.SITE_HOME)
    @Transactional(readOnly = true)
    public BlogDtos.SiteHomeResponse getSiteHome() {
        List<BlogDtos.CategoryResponse> categories = categoryMapper.selectAllOrderByName()
                .stream()
                .map(category -> new BlogDtos.CategoryResponse(
                        category.getId(),
                        category.getName(),
                        category.getSlug(),
                        category.getDescription(),
                        postMapper.countByCategoryIdAndStatus(category.getId(), PostStatus.PUBLISHED.name())
                ))
                .filter(category -> category.postCount() > 0)
                .toList();

        List<BlogDtos.TagResponse> tags = tagMapper.selectAllOrderByName()
                .stream()
                .map(tag -> new BlogDtos.TagResponse(
                        tag.getId(),
                        tag.getName(),
                        tag.getSlug(),
                        postMapper.countByTagIdAndStatus(tag.getId(), PostStatus.PUBLISHED.name())
                ))
                .filter(tag -> tag.postCount() > 0)
                .toList();

        return new BlogDtos.SiteHomeResponse(
                siteName,
                heroTitle,
                heroSubtitle,
                new BlogDtos.SiteStats(
                        postMapper.countByStatus(PostStatus.PUBLISHED.name()),
                        categories.size(),
                        tags.size(),
                        commentMapper.countByStatus(CommentStatus.APPROVED.name())
                ),
                postMapper.selectTopFeaturedPublished(PostStatus.PUBLISHED.name(), 4)
                        .stream()
                        .map(this::toPostSummary)
                        .toList(),
                postMapper.selectTopPublished(PostStatus.PUBLISHED.name(), 6)
                        .stream()
                        .map(this::toPostSummary)
                        .toList(),
                categories,
                tags
        );
    }

    @Cacheable(value = CacheNames.PUBLIC_POST_LIST, key = "T(java.lang.String).format('%s|%s|%s|%s|%s', #keyword, #categorySlug, #tagSlug, #page, #pageSize)")
    @Transactional(readOnly = true)
    public PageResponse<BlogDtos.PostSummaryResponse> listPosts(
            String keyword,
            String categorySlug,
            String tagSlug,
            int page,
            int pageSize
    ) {
        int normalizedPage = normalizePage(page);
        int normalizedPageSize = normalizePageSize(pageSize);
        long total = postMapper.countPublicPosts(keyword, categorySlug, tagSlug);

        List<BlogDtos.PostSummaryResponse> records = postMapper.selectPublicPosts(
                        keyword,
                        categorySlug,
                        tagSlug,
                        normalizedPage * normalizedPageSize,
                        normalizedPageSize
                )
                .stream()
                .map(this::toPostSummary)
                .toList();

        return buildPageResponse(records, normalizedPage, normalizedPageSize, total);
    }

    @Transactional
    public BlogDtos.PostDetailResponse getPostDetail(String slug, boolean trackView) {
        Post post = postMapper.selectBySlugAndStatus(slug, PostStatus.PUBLISHED.name());
        if (post == null) {
            throw new BusinessException("Post not found");
        }

        if (trackView) {
            postMapper.incrementViewCount(post.getId());
            post.setViewCount(post.getViewCount() + 1);
            evictPublicCaches();
        }

        List<BlogDtos.CommentResponse> comments = commentMapper
                .selectByPostIdAndStatusOrderByCreatedAtAsc(post.getId(), CommentStatus.APPROVED.name())
                .stream()
                .map(comment -> new BlogDtos.CommentResponse(
                        comment.getId(),
                        comment.getNickname(),
                        comment.getContent(),
                        comment.getCreatedAt()
                ))
                .toList();

        return new BlogDtos.PostDetailResponse(
                post.getId(),
                post.getTitle(),
                post.getSlug(),
                post.getSummary(),
                post.getCoverImage(),
                post.getContent(),
                post.getCategoryName(),
                post.getCategorySlug(),
                tagMapper.selectTagsByPostId(post.getId()).stream().map(tag -> tag.getName()).toList(),
                post.getViewCount(),
                post.getPublishedAt(),
                post.isFeatured(),
                post.isAllowComment(),
                comments
        );
    }

    @Transactional
    public void createComment(AuthenticatedUser authenticatedUser, BlogDtos.CommentCreateRequest request) {
        if (authenticatedUser == null) {
            throw new BusinessException("Login required");
        }

        UserAccount user = userAccountMapper.selectById(authenticatedUser.getId());
        if (user == null) {
            throw new BusinessException("User not found");
        }

        Post post = postMapper.selectById(request.postId());
        if (post == null || post.getStatus() != PostStatus.PUBLISHED) {
            throw new BusinessException("Post not found or not published");
        }

        if (!post.isAllowComment()) {
            throw new BusinessException("Comments are disabled for this post");
        }

        Comment comment = new Comment();
        comment.setPostId(post.getId());
        comment.setUserId(user.getId());
        comment.setNickname(user.getDisplayName());
        comment.setEmail(user.getEmail());
        comment.setContent(request.content().trim());
        comment.setStatus(CommentStatus.PENDING);
        commentMapper.insert(comment);
    }

    private BlogDtos.PostSummaryResponse toPostSummary(Post post) {
        return new BlogDtos.PostSummaryResponse(
                post.getId(),
                post.getTitle(),
                post.getSlug(),
                post.getSummary(),
                post.getCoverImage(),
                post.getCategoryName(),
                post.getCategorySlug(),
                tagMapper.selectTagsByPostId(post.getId()).stream().map(tag -> tag.getName()).collect(Collectors.toList()),
                post.getViewCount(),
                post.getPublishedAt(),
                post.isFeatured()
        );
    }

    private PageResponse<BlogDtos.PostSummaryResponse> buildPageResponse(
            List<BlogDtos.PostSummaryResponse> records,
            int normalizedPage,
            int pageSize,
            long total
    ) {
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / pageSize);
        return new PageResponse<>(
                records,
                normalizedPage + 1,
                pageSize,
                total,
                totalPages,
                normalizedPage + 1 < totalPages
        );
    }

    private int normalizePage(int page) {
        return Math.max(page, 1) - 1;
    }

    private int normalizePageSize(int pageSize) {
        return Math.min(Math.max(pageSize, 1), 50);
    }

    private void evictPublicCaches() {
        clearCache(CacheNames.SITE_HOME);
        clearCache(CacheNames.PUBLIC_POST_LIST);
    }

    private void clearCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }
}
