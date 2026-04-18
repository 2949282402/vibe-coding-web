package com.hejulian.blog.controller.admin;

import com.hejulian.blog.common.ApiResponse;
import com.hejulian.blog.common.PageResponse;
import com.hejulian.blog.dto.AdminDtos;
import com.hejulian.blog.service.AdminBlogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/posts")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminPostController {

    private final AdminBlogService adminBlogService;

    @GetMapping
    public ApiResponse<PageResponse<AdminDtos.AdminPostListResponse>> listPosts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        return ApiResponse.success(adminBlogService.listPosts(keyword, status, categoryId, page, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminDtos.AdminPostDetailResponse> getPost(@PathVariable Long id) {
        return ApiResponse.success(adminBlogService.getPost(id));
    }

    @PostMapping
    public ApiResponse<AdminDtos.AdminPostDetailResponse> createPost(
            @Valid @RequestBody AdminDtos.PostSaveRequest request
    ) {
        return ApiResponse.success("Post created successfully", adminBlogService.savePost(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<AdminDtos.AdminPostDetailResponse> updatePost(
            @PathVariable Long id,
            @Valid @RequestBody AdminDtos.PostSaveRequest request
    ) {
        AdminDtos.PostSaveRequest payload = new AdminDtos.PostSaveRequest(
                id,
                request.title(),
                request.slug(),
                request.summary(),
                request.coverImage(),
                request.content(),
                request.status(),
                request.featured(),
                request.allowComment(),
                request.categoryId(),
                request.tagIds()
        );
        return ApiResponse.success("Post updated successfully", adminBlogService.savePost(payload));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deletePost(@PathVariable Long id) {
        adminBlogService.deletePost(id);
        return ApiResponse.success("Post deleted successfully", null);
    }
}
