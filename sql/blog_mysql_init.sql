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
      content_hash VARCHAR(64) NULL,
      embedding_json LONGTEXT NULL,
      embedding_model VARCHAR(64) NULL,
      embedding_dimensions INT NULL,
      published_at DATETIME NULL,
      created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
      updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
      UNIQUE KEY uk_rag_chunks_post_chunk (post_id, chunk_index),
      UNIQUE KEY uk_rag_chunks_post_hash (post_id, content_hash),
      KEY idx_rag_chunks_post_id (post_id),
      KEY idx_rag_chunks_content_hash (content_hash),
      KEY idx_rag_chunks_post_slug (post_slug),
      CONSTRAINT fk_rag_chunks_post FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE
  ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS rag_chat_messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id VARCHAR(64) NOT NULL,
    user_id BIGINT NULL,
    role VARCHAR(16) NOT NULL,
    content LONGTEXT NOT NULL,
    answer_mode VARCHAR(16) NULL,
    citations_json LONGTEXT NULL,
    sources_json LONGTEXT NULL,
    variants_json LONGTEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_rag_chat_messages_user_session (user_id, session_id),
    KEY idx_rag_chat_messages_session_id (session_id),
    KEY idx_rag_chat_messages_created_at (created_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS rag_chat_sessions (
    session_id VARCHAR(64) PRIMARY KEY,
    user_id BIGINT NULL,
    title VARCHAR(160) NOT NULL,
    preview LONGTEXT NULL,
    message_count INT NOT NULL DEFAULT 0,
    manual_title TINYINT(1) NOT NULL DEFAULT 0,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_rag_chat_sessions_user_updated (user_id, updated_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS agent_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    session_id VARCHAR(128) NOT NULL,
    task_type VARCHAR(32) NOT NULL,
    title VARCHAR(160) NOT NULL,
    goal VARCHAR(2000) NOT NULL,
    status VARCHAR(32) NOT NULL,
    execution_mode VARCHAR(32) NOT NULL,
    search_scope VARCHAR(32) NOT NULL,
    allow_draft_write TINYINT(1) NOT NULL DEFAULT 0,
    current_step INT NOT NULL DEFAULT 0,
    final_output_summary LONGTEXT NULL,
    error_message VARCHAR(1000) NULL,
    started_at DATETIME NULL,
    completed_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_agent_task_user_id (user_id),
    KEY idx_agent_task_user_status (user_id, status),
    KEY idx_agent_task_created_at (created_at),
    CONSTRAINT fk_agent_task_user FOREIGN KEY (user_id) REFERENCES user_accounts (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS agent_task_step (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    step_index INT NOT NULL,
    agent_role VARCHAR(32) NOT NULL,
    step_name VARCHAR(120) NOT NULL,
    status VARCHAR(32) NOT NULL,
    input_summary VARCHAR(500) NULL,
    output_summary LONGTEXT NULL,
    retry_count INT NULL DEFAULT 0,
    latency_ms BIGINT NULL,
    started_at DATETIME NULL,
    completed_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_agent_task_step_task_id (task_id, step_index),
    CONSTRAINT fk_agent_task_step_task FOREIGN KEY (task_id) REFERENCES agent_task (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS agent_task_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    step_id BIGINT NULL,
    event_type VARCHAR(32) NOT NULL,
    agent_role VARCHAR(32) NOT NULL,
    payload_json LONGTEXT NULL,
    payload_summary VARCHAR(500) NULL,
    status VARCHAR(64) NULL,
    latency_ms BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_agent_task_event_task_id (task_id, id),
    CONSTRAINT fk_agent_task_event_task FOREIGN KEY (task_id) REFERENCES agent_task (id) ON DELETE CASCADE,
    CONSTRAINT fk_agent_task_event_step FOREIGN KEY (step_id) REFERENCES agent_task_step (id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS agent_tool_call (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    step_id BIGINT NULL,
    tool_name VARCHAR(80) NOT NULL,
    permission_level VARCHAR(16) NOT NULL,
    request_json LONGTEXT NULL,
    response_summary VARCHAR(300) NULL,
    success TINYINT(1) NULL,
    error_message VARCHAR(1000) NULL,
    latency_ms BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_agent_tool_call_task_id (task_id, id),
    CONSTRAINT fk_agent_tool_call_task FOREIGN KEY (task_id) REFERENCES agent_task (id) ON DELETE CASCADE,
    CONSTRAINT fk_agent_tool_call_step FOREIGN KEY (step_id) REFERENCES agent_task_step (id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS agent_memory (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    memory_scope VARCHAR(32) NOT NULL,
    topic_key VARCHAR(200) NOT NULL,
    memory_type VARCHAR(32) NOT NULL,
    content LONGTEXT NOT NULL,
    content_summary VARCHAR(500) NULL,
    confidence_score DECIMAL(5,4) NULL,
    source_type VARCHAR(64) NULL,
    source_ref_id BIGINT NULL,
    is_pinned TINYINT(1) NOT NULL DEFAULT 0,
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    last_hit_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_agent_memory_user_id_scope (user_id, memory_scope),
    KEY idx_agent_memory_user_topic (user_id, topic_key),
    CONSTRAINT fk_agent_memory_user FOREIGN KEY (user_id) REFERENCES user_accounts (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS agent_memory_hit (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    memory_id BIGINT NOT NULL,
    hit_reason VARCHAR(255) NULL,
    used_in_step VARCHAR(64) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_agent_memory_hit_task_id (task_id),
    KEY idx_agent_memory_hit_memory_id (memory_id),
    CONSTRAINT fk_agent_memory_hit_task FOREIGN KEY (task_id) REFERENCES agent_task (id) ON DELETE CASCADE,
    CONSTRAINT fk_agent_memory_hit_memory FOREIGN KEY (memory_id) REFERENCES agent_memory (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS agent_eval_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    score_overall INT NULL,
    score_grounding INT NULL,
    score_helpfulness INT NULL,
    score_style INT NULL,
    issue_types VARCHAR(500) NULL,
    feedback_note TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_agent_eval_task_id (task_id),
    KEY idx_agent_eval_user_id (user_id),
    CONSTRAINT fk_agent_eval_record_task FOREIGN KEY (task_id) REFERENCES agent_task (id) ON DELETE CASCADE,
    CONSTRAINT fk_agent_eval_record_user FOREIGN KEY (user_id) REFERENCES user_accounts (id) ON DELETE CASCADE
) ENGINE=InnoDB;
