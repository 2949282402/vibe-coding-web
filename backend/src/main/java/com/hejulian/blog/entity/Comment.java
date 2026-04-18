package com.hejulian.blog.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Comment extends BaseEntity {

    private Long postId;

    private String postTitle;

    private String nickname;

    private String email;

    private String content;

    private CommentStatus status;
}
