package com.hejulian.blog.service;

import com.hejulian.blog.common.SlugUtils;
import com.hejulian.blog.entity.Category;
import com.hejulian.blog.entity.Comment;
import com.hejulian.blog.entity.CommentStatus;
import com.hejulian.blog.entity.Post;
import com.hejulian.blog.entity.PostStatus;
import com.hejulian.blog.entity.Role;
import com.hejulian.blog.entity.Tag;
import com.hejulian.blog.entity.UserAccount;
import com.hejulian.blog.mapper.CategoryMapper;
import com.hejulian.blog.mapper.CommentMapper;
import com.hejulian.blog.mapper.PostMapper;
import com.hejulian.blog.mapper.TagMapper;
import com.hejulian.blog.mapper.UserAccountMapper;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserAccountMapper userAccountMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        initAdmin();
        initDemoContent();
    }

    private void initAdmin() {
        if (userAccountMapper.selectByUsername("admin") != null) {
            return;
        }

        UserAccount admin = new UserAccount();
        admin.setUsername("admin");
        admin.setDisplayName("Site Administrator");
        admin.setPassword(passwordEncoder.encode("Admin123!"));
        admin.setRole(Role.ADMIN);
        userAccountMapper.insert(admin);
    }

    private void initDemoContent() {
        if (postMapper.countAll() > 0) {
            return;
        }

        Category engineering = saveCategory("Engineering", "Architecture, coding practices, and delivery notes.");
        Category product = saveCategory("Product", "Product thinking, iteration rhythm, and user experience.");

        Tag vue = saveTag("Vue 3");
        Tag springBoot = saveTag("Spring Boot");
        Tag architecture = saveTag("Architecture");
        Tag delivery = saveTag("Delivery");

        Post firstPost = savePost(
                "Build a Personal Blog That Can Evolve",
                "A blog should not only publish content, it should support structure, search, and long-term iteration.",
                "<p>A maintainable blog system should separate presentation, administration, data storage, and deployment.</p>"
                        + "<p>That makes it easier to evolve features such as search, AI enrichment, object storage, and analytics later.</p>",
                "https://images.unsplash.com/photo-1498050108023-c5249f4df085?auto=format&fit=crop&w=1200&q=80",
                engineering.getId(),
                List.of(vue.getId(), springBoot.getId(), architecture.getId()),
                true,
                LocalDateTime.now().minusDays(3)
        );

        Post secondPost = savePost(
                "A Weekly Delivery Rhythm for Solo Builders",
                "Stable output matters more than short bursts of speed. A predictable weekly cadence reduces churn.",
                "<p>For independent development, the key is to keep requirements, implementation, verification, and deployment connected.</p>"
                        + "<p>Deliver a visible increment every week and keep the platform easy to operate.</p>",
                "https://images.unsplash.com/photo-1455390582262-044cdead277a?auto=format&fit=crop&w=1200&q=80",
                product.getId(),
                List.of(delivery.getId(), architecture.getId()),
                false,
                LocalDateTime.now().minusDays(1)
        );

        Comment approved = new Comment();
        approved.setPostId(firstPost.getId());
        approved.setNickname("Julian");
        approved.setEmail("julian@example.com");
        approved.setContent("This structure is well suited for a long-running personal blog.");
        approved.setStatus(CommentStatus.APPROVED);
        commentMapper.insert(approved);

        Comment pending = new Comment();
        pending.setPostId(secondPost.getId());
        pending.setNickname("Visitor");
        pending.setEmail("visitor@example.com");
        pending.setContent("Please add a Markdown editor in a later iteration.");
        pending.setStatus(CommentStatus.PENDING);
        commentMapper.insert(pending);
    }

    private Category saveCategory(String name, String description) {
        Category category = new Category();
        category.setName(name);
        category.setSlug(SlugUtils.toSlug(name));
        category.setDescription(description);
        categoryMapper.insert(category);
        return category;
    }

    private Tag saveTag(String name) {
        Tag tag = new Tag();
        tag.setName(name);
        tag.setSlug(SlugUtils.toSlug(name));
        tagMapper.insert(tag);
        return tag;
    }

    private Post savePost(String title,
                          String summary,
                          String content,
                          String coverImage,
                          Long categoryId,
                          List<Long> tagIds,
                          boolean featured,
                          LocalDateTime publishedAt) {
        Post post = new Post();
        post.setTitle(title);
        post.setSlug(SlugUtils.toSlug(title));
        post.setSummary(summary);
        post.setContent(content);
        post.setCoverImage(coverImage);
        post.setCategoryId(categoryId);
        post.setFeatured(featured);
        post.setAllowComment(true);
        post.setStatus(PostStatus.PUBLISHED);
        post.setPublishedAt(publishedAt);
        post.setViewCount(featured ? 128 : 64);
        postMapper.insert(post);
        postMapper.insertPostTags(post.getId(), tagIds);
        return post;
    }
}
