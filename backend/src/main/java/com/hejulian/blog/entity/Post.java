package com.hejulian.blog.entity;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Post extends BaseEntity {

    private String title;

    private String slug;

    private String summary;

    private String coverImage;

    private String content;

    private PostStatus status;

    private boolean allowComment = true;

    private boolean featured = false;

    private long viewCount = 0;

    private LocalDateTime publishedAt;

    private Long categoryId;

    private String categoryName;

    private String categorySlug;

    private Set<Tag> tags = new LinkedHashSet<>();
}

