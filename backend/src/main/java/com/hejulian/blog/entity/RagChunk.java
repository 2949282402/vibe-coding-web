package com.hejulian.blog.entity;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RagChunk {

    private Long id;
    private Long postId;
    private String postTitle;
    private String postSlug;
    private Integer chunkIndex;
    private String content;
    private String embeddingJson;
    private String embeddingModel;
    private Integer embeddingDimensions;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
