package com.hejulian.blog.controller.admin;

import com.hejulian.blog.common.ApiResponse;
import com.hejulian.blog.dto.AdminDtos;
import com.hejulian.blog.dto.BlogDtos;
import com.hejulian.blog.service.AdminBlogService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminTaxonomyController {

    private final AdminBlogService adminBlogService;

    @GetMapping("/categories")
    public ApiResponse<List<BlogDtos.CategoryResponse>> categories() {
        return ApiResponse.success(adminBlogService.listCategories());
    }

    @PostMapping("/categories")
    public ApiResponse<BlogDtos.CategoryResponse> createCategory(
            @Valid @RequestBody AdminDtos.TaxonomySaveRequest request
    ) {
        return ApiResponse.success("Category created successfully", adminBlogService.saveCategory(null, request));
    }

    @PutMapping("/categories/{id}")
    public ApiResponse<BlogDtos.CategoryResponse> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody AdminDtos.TaxonomySaveRequest request
    ) {
        return ApiResponse.success("Category updated successfully", adminBlogService.saveCategory(id, request));
    }

    @DeleteMapping("/categories/{id}")
    public ApiResponse<Void> deleteCategory(@PathVariable Long id) {
        adminBlogService.deleteCategory(id);
        return ApiResponse.success("Category deleted successfully", null);
    }

    @GetMapping("/tags")
    public ApiResponse<List<BlogDtos.TagResponse>> tags() {
        return ApiResponse.success(adminBlogService.listTags());
    }

    @PostMapping("/tags")
    public ApiResponse<BlogDtos.TagResponse> createTag(@Valid @RequestBody AdminDtos.TaxonomySaveRequest request) {
        return ApiResponse.success("Tag created successfully", adminBlogService.saveTag(null, request));
    }

    @PutMapping("/tags/{id}")
    public ApiResponse<BlogDtos.TagResponse> updateTag(
            @PathVariable Long id,
            @Valid @RequestBody AdminDtos.TaxonomySaveRequest request
    ) {
        return ApiResponse.success("Tag updated successfully", adminBlogService.saveTag(id, request));
    }

    @DeleteMapping("/tags/{id}")
    public ApiResponse<Void> deleteTag(@PathVariable Long id) {
        adminBlogService.deleteTag(id);
        return ApiResponse.success("Tag deleted successfully", null);
    }
}
