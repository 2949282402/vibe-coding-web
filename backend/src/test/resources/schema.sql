CREATE TABLE user_accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    password VARCHAR(255) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    role VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_user_accounts_username ON user_accounts (username);

CREATE TABLE categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_categories_slug ON categories (slug);

CREATE TABLE tags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(120) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_tags_slug ON tags (slug);

CREATE TABLE posts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    slug VARCHAR(220) NOT NULL,
    summary VARCHAR(1000) NOT NULL,
    cover_image VARCHAR(500),
    content CLOB NOT NULL,
    status VARCHAR(32) NOT NULL,
    allow_comment BOOLEAN NOT NULL DEFAULT TRUE,
    featured BOOLEAN NOT NULL DEFAULT FALSE,
    view_count BIGINT NOT NULL DEFAULT 0,
    published_at TIMESTAMP NULL,
    category_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_posts_category FOREIGN KEY (category_id) REFERENCES categories (id)
);

CREATE UNIQUE INDEX uk_posts_slug ON posts (slug);
CREATE INDEX idx_posts_status_published_at ON posts (status, published_at);
CREATE INDEX idx_posts_category_id ON posts (category_id);

CREATE TABLE post_tags (
    post_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    PRIMARY KEY (post_id, tag_id),
    CONSTRAINT fk_post_tags_post FOREIGN KEY (post_id) REFERENCES posts (id),
    CONSTRAINT fk_post_tags_tag FOREIGN KEY (tag_id) REFERENCES tags (id)
);

CREATE INDEX idx_post_tags_tag_id ON post_tags (tag_id);

CREATE TABLE comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    nickname VARCHAR(80) NOT NULL,
    email VARCHAR(120) NOT NULL,
    content VARCHAR(1000) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comments_post FOREIGN KEY (post_id) REFERENCES posts (id)
);

CREATE INDEX idx_comments_post_id ON comments (post_id);
CREATE INDEX idx_comments_status ON comments (status);

CREATE TABLE rag_chunks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    post_title VARCHAR(200) NOT NULL,
    post_slug VARCHAR(220) NOT NULL,
    chunk_index INT NOT NULL,
    content CLOB NOT NULL,
    embedding_json CLOB,
    embedding_model VARCHAR(64),
    embedding_dimensions INT,
    published_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_rag_chunks_post FOREIGN KEY (post_id) REFERENCES posts (id)
);

CREATE UNIQUE INDEX uk_rag_chunks_post_chunk ON rag_chunks (post_id, chunk_index);
CREATE INDEX idx_rag_chunks_post_id ON rag_chunks (post_id);
CREATE INDEX idx_rag_chunks_post_slug ON rag_chunks (post_slug);

CREATE TABLE rag_chat_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL,
    role VARCHAR(16) NOT NULL,
    content CLOB NOT NULL,
    answer_mode VARCHAR(16),
    citations_json CLOB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_rag_chat_messages_session_id ON rag_chat_messages (session_id);
CREATE INDEX idx_rag_chat_messages_created_at ON rag_chat_messages (created_at);
