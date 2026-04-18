package com.hejulian.blog.service;

import com.hejulian.blog.common.CacheNames;
import com.hejulian.blog.common.PageResponse;
import com.hejulian.blog.common.SlugUtils;
import com.hejulian.blog.dto.AdminDtos;
import com.hejulian.blog.dto.BlogDtos;
import com.hejulian.blog.entity.CommentStatus;
import com.hejulian.blog.entity.Post;
import com.hejulian.blog.entity.PostStatus;
import com.hejulian.blog.entity.Tag;
import com.hejulian.blog.exception.BusinessException;
import com.hejulian.blog.mapper.CategoryMapper;
import com.hejulian.blog.mapper.CommentMapper;
import com.hejulian.blog.mapper.PostMapper;
import com.hejulian.blog.mapper.TagMapper;
import com.hejulian.blog.rag.application.RagIndexingApplicationService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminBlogService {

    private final PostMapper postMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final CommentMapper commentMapper;
    private final RagIndexingApplicationService ragIndexingApplicationService;

    public AdminBlogService(PostMapper postMapper,
                            CategoryMapper categoryMapper,
                            TagMapper tagMapper,
                            CommentMapper commentMapper,
                            RagIndexingApplicationService ragIndexingApplicationService) {
        this.postMapper = postMapper;
        this.categoryMapper = categoryMapper;
        this.tagMapper = tagMapper;
        this.commentMapper = commentMapper;
        this.ragIndexingApplicationService = ragIndexingApplicationService;
    }

    @Transactional(readOnly = true)
    public AdminDtos.DashboardResponse getDashboard() {
        return new AdminDtos.DashboardResponse(
                postMapper.countAll(),
                categoryMapper.countAll(),
                tagMapper.countAll(),
                commentMapper.countByStatus(CommentStatus.PENDING.name()),
                commentMapper.countByStatus(CommentStatus.APPROVED.name()),
                postMapper.selectRecentUpdated(6)
                        .stream()
                        .map(post -> new AdminDtos.RecentPostResponse(post.getId(), post.getTitle(), post.getUpdatedAt()))
                        .toList(),
                commentMapper.selectRecentComments(6)
                        .stream()
                        .map(comment -> new AdminDtos.RecentCommentResponse(
                                comment.getId(),
                                comment.getNickname(),
                                comment.getPostTitle(),
                                comment.getCreatedAt()
                        ))
                        .toList()
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminDtos.AdminPostListResponse> listPosts(
            String keyword,
            String status,
            Long categoryId,
            int page,
            int pageSize
    ) {
        int normalizedPage = normalizePage(page);
        int normalizedPageSize = normalizePageSize(pageSize);
        long total = postMapper.countAdminPosts(keyword, normalizeStatus(status), categoryId);

        List<AdminDtos.AdminPostListResponse> records = postMapper
                .selectAdminPosts(keyword, normalizeStatus(status), categoryId, normalizedPage * normalizedPageSize, normalizedPageSize)
                .stream()
                .map(post -> new AdminDtos.AdminPostListResponse(
                        post.getId(),
                        post.getTitle(),
                        post.getSlug(),
                        post.getCategoryName(),
                        post.getStatus().name(),
                        post.getViewCount(),
                        post.getUpdatedAt()
                ))
                .toList();

        return buildPageResponse(records, normalizedPage, normalizedPageSize, total);
    }

    @Transactional(readOnly = true)
    public AdminDtos.AdminPostDetailResponse getPost(Long id) {
        Post post = postMapper.selectById(id);
        if (post == null) {
            throw new BusinessException("Post not found");
        }

        List<Long> tagIds = tagMapper.selectTagsByPostId(post.getId())
                .stream()
                .map(Tag::getId)
                .toList();

        return new AdminDtos.AdminPostDetailResponse(
                post.getId(),
                post.getTitle(),
                post.getSlug(),
                post.getSummary(),
                post.getCoverImage(),
                post.getContent(),
                post.getStatus().name(),
                post.isFeatured(),
                post.isAllowComment(),
                post.getCategoryId(),
                tagIds
        );
    }

    @Transactional
    @CacheEvict(value = {CacheNames.SITE_HOME, CacheNames.PUBLIC_POST_LIST}, allEntries = true)
    public AdminDtos.AdminPostDetailResponse savePost(AdminDtos.PostSaveRequest request) {
        if (categoryMapper.countById(request.categoryId()) == 0) {
            throw new BusinessException("Category not found");
        }

        List<Tag> tags = tagMapper.selectByIds(request.tagIds());
        if (tags.size() != request.tagIds().size()) {
            throw new BusinessException("One or more tags are invalid");
        }

        Post post = request.id() == null ? new Post() : postMapper.selectById(request.id());
        if (request.id() != null && post == null) {
            throw new BusinessException("Post not found");
        }

        PostStatus nextStatus = parsePostStatus(request.status());
        boolean firstPublish = nextStatus == PostStatus.PUBLISHED && post.getPublishedAt() == null;

        post.setTitle(request.title().trim());
        post.setSlug(generatePostSlug(request.slug(), request.title(), post.getId()));
        post.setSummary(request.summary().trim());
        post.setCoverImage(StringUtils.hasText(request.coverImage()) ? request.coverImage().trim() : null);
        post.setContent(request.content().trim());
        post.setStatus(nextStatus);
        post.setFeatured(request.featured());
        post.setAllowComment(request.allowComment());
        post.setCategoryId(request.categoryId());
        if (firstPublish) {
            post.setPublishedAt(LocalDateTime.now());
        }

        if (post.getId() == null) {
            postMapper.insert(post);
        } else {
            postMapper.update(post);
            postMapper.deletePostTags(post.getId());
        }
        postMapper.insertPostTags(post.getId(), request.tagIds());
        ragIndexingApplicationService.syncPost(post);
        return getPost(post.getId());
    }

    @Transactional
    @CacheEvict(value = {CacheNames.SITE_HOME, CacheNames.PUBLIC_POST_LIST}, allEntries = true)
    public void deletePost(Long id) {
        if (postMapper.selectById(id) == null) {
            throw new BusinessException("Post not found");
        }
        commentMapper.deleteByPostId(id);
        postMapper.deletePostTags(id);
        postMapper.deleteById(id);
        ragIndexingApplicationService.removePost(id);
    }

    @Cacheable(CacheNames.CATEGORY_LIST)
    @Transactional(readOnly = true)
    public List<BlogDtos.CategoryResponse> listCategories() {
        return categoryMapper.selectAllOrderByName()
                .stream()
                .map(category -> new BlogDtos.CategoryResponse(
                        category.getId(),
                        category.getName(),
                        category.getSlug(),
                        category.getDescription(),
                        postMapper.countByCategoryIdAndStatus(category.getId(), PostStatus.PUBLISHED.name())
                ))
                .toList();
    }

    @Transactional
    @CacheEvict(value = {CacheNames.CATEGORY_LIST, CacheNames.SITE_HOME, CacheNames.PUBLIC_POST_LIST}, allEntries = true)
    public BlogDtos.CategoryResponse saveCategory(Long id, AdminDtos.TaxonomySaveRequest request) {
        var category = id == null ? new com.hejulian.blog.entity.Category() : categoryMapper.selectById(id);
        if (id != null && category == null) {
            throw new BusinessException("Category not found");
        }

        category.setName(request.name().trim());
        category.setSlug(generateSlug(
                request.slug(),
                request.name(),
                slug -> id == null ? categoryMapper.countBySlug(slug) > 0 : categoryMapper.countBySlugAndIdNot(slug, id) > 0
        ));
        category.setDescription(StringUtils.hasText(request.description()) ? request.description().trim() : null);

        if (id == null) {
            categoryMapper.insert(category);
        } else {
            category.setId(id);
            categoryMapper.update(category);
        }

        return new BlogDtos.CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                postMapper.countByCategoryIdAndStatus(category.getId(), PostStatus.PUBLISHED.name())
        );
    }

    @Transactional
    @CacheEvict(value = {CacheNames.CATEGORY_LIST, CacheNames.SITE_HOME, CacheNames.PUBLIC_POST_LIST}, allEntries = true)
    public void deleteCategory(Long id) {
        if (categoryMapper.countById(id) == 0) {
            throw new BusinessException("Category not found");
        }
        if (postMapper.countByCategoryId(id) > 0) {
            throw new BusinessException("Category still has associated posts");
        }
        categoryMapper.deleteById(id);
    }

    @Cacheable(CacheNames.TAG_LIST)
    @Transactional(readOnly = true)
    public List<BlogDtos.TagResponse> listTags() {
        return tagMapper.selectAllOrderByName()
                .stream()
                .map(tag -> new BlogDtos.TagResponse(
                        tag.getId(),
                        tag.getName(),
                        tag.getSlug(),
                        postMapper.countByTagIdAndStatus(tag.getId(), PostStatus.PUBLISHED.name())
                ))
                .toList();
    }

    @Transactional
    @CacheEvict(value = {CacheNames.TAG_LIST, CacheNames.SITE_HOME, CacheNames.PUBLIC_POST_LIST}, allEntries = true)
    public BlogDtos.TagResponse saveTag(Long id, AdminDtos.TaxonomySaveRequest request) {
        var tag = id == null ? new Tag() : tagMapper.selectById(id);
        if (id != null && tag == null) {
            throw new BusinessException("Tag not found");
        }

        tag.setName(request.name().trim());
        tag.setSlug(generateSlug(
                request.slug(),
                request.name(),
                slug -> id == null ? tagMapper.countBySlug(slug) > 0 : tagMapper.countBySlugAndIdNot(slug, id) > 0
        ));

        if (id == null) {
            tagMapper.insert(tag);
        } else {
            tag.setId(id);
            tagMapper.update(tag);
        }

        return new BlogDtos.TagResponse(
                tag.getId(),
                tag.getName(),
                tag.getSlug(),
                postMapper.countByTagIdAndStatus(tag.getId(), PostStatus.PUBLISHED.name())
        );
    }

    @Transactional
    @CacheEvict(value = {CacheNames.TAG_LIST, CacheNames.SITE_HOME, CacheNames.PUBLIC_POST_LIST}, allEntries = true)
    public void deleteTag(Long id) {
        if (tagMapper.countById(id) == 0) {
            throw new BusinessException("Tag not found");
        }
        if (postMapper.countByTagId(id) > 0) {
            throw new BusinessException("Tag still has associated posts");
        }
        tagMapper.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<AdminDtos.AdminCommentResponse> listComments() {
        return commentMapper.selectAllOrderByCreatedAtDesc()
                .stream()
                .map(comment -> new AdminDtos.AdminCommentResponse(
                        comment.getId(),
                        comment.getNickname(),
                        comment.getEmail(),
                        comment.getContent(),
                        comment.getStatus().name(),
                        comment.getPostTitle(),
                        comment.getCreatedAt()
                ))
                .toList();
    }

    @Transactional
    @CacheEvict(value = CacheNames.SITE_HOME, allEntries = true)
    public void reviewComment(Long id, AdminDtos.CommentReviewRequest request) {
        var comment = commentMapper.selectById(id);
        if (comment == null) {
            throw new BusinessException("Comment not found");
        }
        commentMapper.updateStatus(id, parseCommentStatus(request.status()).name());
    }

    private String generatePostSlug(String requestedSlug, String title, Long id) {
        return generateSlug(
                requestedSlug,
                title,
                slug -> id == null ? postMapper.countBySlug(slug) > 0 : postMapper.countBySlugAndIdNot(slug, id) > 0
        );
    }

    private String generateSlug(String requestedSlug, String fallback, java.util.function.Predicate<String> existsChecker) {
        String base = SlugUtils.toSlug(StringUtils.hasText(requestedSlug) ? requestedSlug : fallback);
        String candidate = base;
        int index = 1;
        while (existsChecker.test(candidate)) {
            candidate = base + "-" + index++;
        }
        return candidate;
    }

    private PostStatus parsePostStatus(String status) {
        try {
            return PostStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new BusinessException("Invalid post status");
        }
    }

    private CommentStatus parseCommentStatus(String status) {
        try {
            return CommentStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new BusinessException("Invalid comment status");
        }
    }

    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        return parsePostStatus(status).name();
    }

    private int normalizePage(int page) {
        return Math.max(page, 1) - 1;
    }

    private int normalizePageSize(int pageSize) {
        return Math.min(Math.max(pageSize, 1), 50);
    }

    private <T> PageResponse<T> buildPageResponse(List<T> records, int normalizedPage, int pageSize, long total) {
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
}
