package com.hejulian.blog.controller.admin;

import com.hejulian.blog.common.ApiResponse;
import com.hejulian.blog.dto.AdminDtos;
import com.hejulian.blog.service.AdminBlogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminBlogService adminBlogService;

    @GetMapping
    public ApiResponse<AdminDtos.DashboardResponse> dashboard() {
        return ApiResponse.success(adminBlogService.getDashboard());
    }
}

