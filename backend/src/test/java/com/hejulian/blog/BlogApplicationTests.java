package com.hejulian.blog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hejulian.blog.entity.Comment;
import com.hejulian.blog.entity.CommentStatus;
import com.hejulian.blog.entity.Post;
import com.hejulian.blog.entity.PostStatus;
import com.hejulian.blog.entity.Tag;
import com.hejulian.blog.mapper.CategoryMapper;
import com.hejulian.blog.mapper.CommentMapper;
import com.hejulian.blog.mapper.PostMapper;
import com.hejulian.blog.mapper.TagMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BlogApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private TagMapper tagMapper;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Test
    void siteEndpointShouldReturnSeededContent() throws Exception {
        mockMvc.perform(get("/api/public/site"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.siteName").value("HeJulian Blog"))
                .andExpect(jsonPath("$.data.latestPosts.length()").value(2));
    }

    @Test
    void publicPostsShouldSupportKeywordAndPagination() throws Exception {
        Long postId = insertPublishedPost("Docker Compose Guide", "Compose for frontend and backend");

        mockMvc.perform(get("/api/public/posts")
                        .param("keyword", "Docker")
                        .param("page", "1")
                        .param("pageSize", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(postId))
                .andExpect(jsonPath("$.data.records[0].title").value("Docker Compose Guide"))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(1))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1));
    }

    @Test
    void loginShouldReturnJwtToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin",
                                  "password": "Admin123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.user.username").value("admin"));
    }

    @Test
    void loginShouldReturnUnauthorizedWhenPasswordIsWrong() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin",
                                  "password": "wrong-password"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void adminEndpointsShouldRejectAnonymousAccess() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminPostsShouldSupportKeywordStatusAndPagination() throws Exception {
        String token = loginAndGetToken();
        insertPublishedPost("Admin Search Post", "keyword for admin search");

        mockMvc.perform(get("/api/admin/posts")
                        .header("Authorization", "Bearer " + token)
                        .param("status", "PUBLISHED")
                        .param("keyword", "Admin Search")
                        .param("page", "1")
                        .param("pageSize", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.records[0].status").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.records[0].title").value("Admin Search Post"))
                .andExpect(jsonPath("$.data.page").value(1));
    }

    @Test
    void actuatorHealthShouldBePublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void deletePostShouldAlsoDeleteItsComments() throws Exception {
        String token = loginAndGetToken();
        Long postId = insertPublishedPost("Delete Me", "post that will be removed");

        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setNickname("tester");
        comment.setEmail("tester@example.com");
        comment.setContent("comment");
        comment.setStatus(CommentStatus.PENDING);
        commentMapper.insert(comment);

        mockMvc.perform(delete("/api/admin/posts/{id}", postId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        Assertions.assertNull(postMapper.selectById(postId));
        Assertions.assertNull(commentMapper.selectById(comment.getId()));
    }

    @Test
    void deleteCategoryShouldBeBlockedWhenPostsStillReferenceIt() throws Exception {
        String token = loginAndGetToken();
        Long categoryId = categoryMapper.selectAllOrderByName().getFirst().getId();

        mockMvc.perform(delete("/api/admin/categories/{id}", categoryId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void ragEndpointShouldReturnRetrievedSources() throws Exception {
        insertPublishedPost(
                "RAG Index Design",
                "Chunking published posts into a searchable knowledge base for question answering."
        );

        mockMvc.perform(post("/api/public/rag/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "question": "How does the blog build a searchable knowledge base?",
                                  "topK": 3
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.answer").isNotEmpty())
                .andExpect(jsonPath("$.data.mode").value("retrieval"))
                .andExpect(jsonPath("$.data.sources.length()").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.data.indexedChunks").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    @Test
    void ragEndpointShouldRejectBlankQuestion() throws Exception {
        mockMvc.perform(post("/api/public/rag/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "question": "   "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    private Long insertPublishedPost(String title, String summary) {
        Long categoryId = categoryMapper.selectAllOrderByName().getFirst().getId();
        List<Tag> tags = tagMapper.selectAllOrderByName();

        Post post = new Post();
        post.setTitle(title);
        post.setSlug(title.toLowerCase().replace(" ", "-") + "-" + System.nanoTime());
        post.setSummary(summary);
        post.setContent("<p>content</p>");
        post.setStatus(PostStatus.PUBLISHED);
        post.setAllowComment(true);
        post.setFeatured(false);
        post.setCategoryId(categoryId);
        post.setPublishedAt(LocalDateTime.now());
        post.setViewCount(0);
        postMapper.insert(post);
        postMapper.insertPostTags(post.getId(), tags.stream().map(Tag::getId).limit(1).toList());
        return post.getId();
    }

    private String loginAndGetToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin",
                                  "password": "Admin123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.path("data").path("token").asText();
    }
}
