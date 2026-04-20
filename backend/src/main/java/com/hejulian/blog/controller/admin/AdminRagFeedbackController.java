package com.hejulian.blog.controller.admin;

import com.hejulian.blog.common.ApiResponse;
import com.hejulian.blog.common.PageResponse;
import com.hejulian.blog.dto.AdminDtos;
import com.hejulian.blog.service.AdminBlogService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/rag-feedback")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminRagFeedbackController {

    private final AdminBlogService adminBlogService;

    @GetMapping
    public ApiResponse<PageResponse<AdminDtos.RagFeedbackListResponse>> listFeedback(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean helpful,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate feedbackDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate feedbackDateTo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        return ApiResponse.success(adminBlogService.listRagFeedback(keyword, helpful, feedbackDateFrom, feedbackDateTo, page, pageSize));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportFeedback(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean helpful,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate feedbackDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate feedbackDateTo
    ) {
        byte[] csv = adminBlogService.exportRagFeedbackCsv(keyword, helpful, feedbackDateFrom, feedbackDateTo);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("rag-feedback-export.csv").build().toString())
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }
}
