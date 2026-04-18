package com.hejulian.blog.controller.admin;

import com.hejulian.blog.common.ApiResponse;
import com.hejulian.blog.dto.AdminDtos;
import com.hejulian.blog.service.AdminBlogService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/comments")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminCommentController {

    private final AdminBlogService adminBlogService;

    @GetMapping
    public ApiResponse<List<AdminDtos.AdminCommentResponse>> listComments() {
        return ApiResponse.success(adminBlogService.listComments());
    }

    @PutMapping("/{id}/status")
    public ApiResponse<Void> reviewComment(
            @PathVariable Long id,
            @Valid @RequestBody AdminDtos.CommentReviewRequest request
    ) {
        adminBlogService.reviewComment(id, request);
        return ApiResponse.success("Comment status updated", null);
    }
}
