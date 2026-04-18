package com.hejulian.blog.controller;

import com.hejulian.blog.common.ApiResponse;
import com.hejulian.blog.common.PageResponse;
import com.hejulian.blog.dto.BlogDtos;
import com.hejulian.blog.service.PublicBlogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicBlogController {

    private final PublicBlogService publicBlogService;

    @GetMapping("/site")
    public ApiResponse<BlogDtos.SiteHomeResponse> site() {
        return ApiResponse.success(publicBlogService.getSiteHome());
    }

    @GetMapping("/posts")
    public ApiResponse<PageResponse<BlogDtos.PostSummaryResponse>> posts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String categorySlug,
            @RequestParam(required = false) String tagSlug,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        return ApiResponse.success(publicBlogService.listPosts(keyword, categorySlug, tagSlug, page, pageSize));
    }

    @GetMapping("/posts/{slug}")
    public ApiResponse<BlogDtos.PostDetailResponse> postDetail(
            @PathVariable String slug,
            @RequestParam(defaultValue = "true") boolean trackView
    ) {
        return ApiResponse.success(publicBlogService.getPostDetail(slug, trackView));
    }

    @PostMapping("/comments")
    public ApiResponse<Void> createComment(@Valid @RequestBody BlogDtos.CommentCreateRequest request) {
        publicBlogService.createComment(request);
        return ApiResponse.success("Comment submitted and awaiting review", null);
    }
}
