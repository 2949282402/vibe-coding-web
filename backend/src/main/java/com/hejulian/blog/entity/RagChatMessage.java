package com.hejulian.blog.entity;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RagChatMessage {

    private Long id;
    private String sessionId;
    private String role;
    private String content;
    private String answerMode;
    private String citationsJson;
    private LocalDateTime createdAt;
}
