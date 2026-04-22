CREATE DATABASE IF NOT EXISTS hejulian_blog
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE hejulian_blog;

CREATE TABLE IF NOT EXISTS user_accounts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL,
    email VARCHAR(160) NULL,
    password VARCHAR(255) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    role VARCHAR(32) NOT NULL,
    qwen_api_key VARCHAR(512) NULL,
    qwen_chat_model VARCHAR(80) NULL,
    qwen_web_search_enabled TINYINT(1) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_accounts_username (username)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS categories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(120) NOT NULL,
    description VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_categories_slug (slug)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS tags (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(120) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_tags_slug (slug)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS posts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    slug VARCHAR(220) NOT NULL,
    summary VARCHAR(1000) NOT NULL,
    cover_image VARCHAR(500) NULL,
    content LONGTEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    allow_comment TINYINT(1) NOT NULL DEFAULT 1,
    featured TINYINT(1) NOT NULL DEFAULT 0,
    view_count BIGINT NOT NULL DEFAULT 0,
    published_at DATETIME NULL,
    category_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_posts_slug (slug),
    KEY idx_posts_status_published_at (status, published_at),
    KEY idx_posts_category_id (category_id),
    CONSTRAINT fk_posts_category FOREIGN KEY (category_id) REFERENCES categories (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS post_tags (
    post_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    PRIMARY KEY (post_id, tag_id),
    KEY idx_post_tags_tag_id (tag_id),
    CONSTRAINT fk_post_tags_post FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE,
    CONSTRAINT fk_post_tags_tag FOREIGN KEY (tag_id) REFERENCES tags (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS comments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    user_id BIGINT NULL,
    nickname VARCHAR(80) NOT NULL,
    email VARCHAR(120) NOT NULL,
    content VARCHAR(1000) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_comments_post_id (post_id),
    KEY idx_comments_status (status),
    CONSTRAINT fk_comments_post FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS rag_chunks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    post_title VARCHAR(200) NOT NULL,
    post_slug VARCHAR(220) NOT NULL,
    chunk_index INT NOT NULL,
    content LONGTEXT NOT NULL,
    embedding_json LONGTEXT NULL,
    embedding_model VARCHAR(64) NULL,
    embedding_dimensions INT NULL,
    published_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_rag_chunks_post_chunk (post_id, chunk_index),
    KEY idx_rag_chunks_post_id (post_id),
    KEY idx_rag_chunks_post_slug (post_slug),
    CONSTRAINT fk_rag_chunks_post FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS rag_chat_messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id VARCHAR(64) NOT NULL,
    role VARCHAR(16) NOT NULL,
    content LONGTEXT NOT NULL,
    answer_mode VARCHAR(16) NULL,
    citations_json LONGTEXT NULL,
    sources_json LONGTEXT NULL,
    variants_json LONGTEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_rag_chat_messages_session_id (session_id),
    KEY idx_rag_chat_messages_created_at (created_at)
) ENGINE=InnoDB;
