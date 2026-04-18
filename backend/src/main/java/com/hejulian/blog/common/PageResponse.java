package com.hejulian.blog.common;

import java.util.List;
import org.springframework.data.domain.Page;

public record PageResponse<T>(
        List<T> records,
        int page,
        int pageSize,
        long total,
        int totalPages,
        boolean hasNext
) {

    public static <T> PageResponse<T> of(Page<?> page, List<T> records) {
        return new PageResponse<>(
                records,
                page.getNumber() + 1,
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext()
        );
    }
}

