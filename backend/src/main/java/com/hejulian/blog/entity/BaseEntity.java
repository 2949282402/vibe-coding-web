package com.hejulian.blog.entity;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class BaseEntity {

    private Long id;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

