package com.hejulian.blog.rag.infrastructure.persistence;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RagSchemaInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    @Override
    public void run(String... args) {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            String productName = metadata.getDatabaseProductName();
            jdbcTemplate.execute(buildCreateChunkTableSql(productName));
            jdbcTemplate.execute(buildCreateChatSessionTableSql(productName));
            jdbcTemplate.execute(buildCreateChatMessageTableSql(productName));
            ensureColumn(metadata, "rag_chunks", "embedding_json", isH2(productName) ? "CLOB" : "LONGTEXT");
            ensureColumn(metadata, "rag_chunks", "embedding_model", "VARCHAR(64)");
            ensureColumn(metadata, "rag_chunks", "embedding_dimensions", "INT");
            backfillMissingSessions();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to initialize rag_chunks schema", ex);
        }
    }

    private String buildCreateChunkTableSql(String productName) {
        boolean h2 = isH2(productName);
        String contentType = h2 ? "CLOB" : "LONGTEXT";
        String timestampType = h2 ? "TIMESTAMP" : "DATETIME";
        String updateClause = h2 ? "" : " ON UPDATE CURRENT_TIMESTAMP";

        return """
                CREATE TABLE IF NOT EXISTS rag_chunks (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    post_id BIGINT NOT NULL,
                    post_title VARCHAR(200) NOT NULL,
                    post_slug VARCHAR(220) NOT NULL,
                    chunk_index INT NOT NULL,
                    content %s NOT NULL,
                    embedding_json %s NULL,
                    embedding_model VARCHAR(64) NULL,
                    embedding_dimensions INT NULL,
                    published_at %s NULL,
                    created_at %s NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at %s NOT NULL DEFAULT CURRENT_TIMESTAMP%s,
                    CONSTRAINT uk_rag_chunks_post_chunk UNIQUE (post_id, chunk_index),
                    CONSTRAINT fk_rag_chunks_post FOREIGN KEY (post_id) REFERENCES posts (id)
                )
                """.formatted(contentType, contentType, timestampType, timestampType, timestampType, updateClause);
    }

    private String buildCreateChatMessageTableSql(String productName) {
        boolean h2 = isH2(productName);
        String contentType = h2 ? "CLOB" : "LONGTEXT";
        String timestampType = h2 ? "TIMESTAMP" : "DATETIME";

        return """
                CREATE TABLE IF NOT EXISTS rag_chat_messages (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    session_id VARCHAR(64) NOT NULL,
                    role VARCHAR(16) NOT NULL,
                    content %s NOT NULL,
                    answer_mode VARCHAR(16) NULL,
                    citations_json %s NULL,
                    created_at %s NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """.formatted(contentType, contentType, timestampType);
    }

    private String buildCreateChatSessionTableSql(String productName) {
        boolean h2 = isH2(productName);
        String contentType = h2 ? "CLOB" : "LONGTEXT";
        String timestampType = h2 ? "TIMESTAMP" : "DATETIME";
        String booleanType = "BOOLEAN";
        String updateClause = h2 ? "" : " ON UPDATE CURRENT_TIMESTAMP";

        return """
                CREATE TABLE IF NOT EXISTS rag_chat_sessions (
                    session_id VARCHAR(64) PRIMARY KEY,
                    title VARCHAR(160) NOT NULL,
                    preview %s NULL,
                    message_count INT NOT NULL DEFAULT 0,
                    manual_title %s NOT NULL DEFAULT FALSE,
                    deleted %s NOT NULL DEFAULT FALSE,
                    created_at %s NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at %s NOT NULL DEFAULT CURRENT_TIMESTAMP%s
                )
                """.formatted(contentType, booleanType, booleanType, timestampType, timestampType, updateClause);
    }

    private boolean isH2(String productName) {
        return productName != null && productName.toLowerCase().contains("h2");
    }

    private void backfillMissingSessions() {
        jdbcTemplate.execute("""
                INSERT INTO rag_chat_sessions (
                    session_id, title, preview, message_count, manual_title, deleted, created_at, updated_at
                )
                SELECT
                    m.session_id,
                    SUBSTRING(COALESCE(MAX(CASE WHEN m.role = 'user' THEN m.content END), m.session_id), 1, 160),
                    SUBSTRING(MAX(CASE WHEN m.role = 'user' THEN m.content END), 1, 400),
                    COUNT(*),
                    FALSE,
                    FALSE,
                    MIN(m.created_at),
                    MAX(m.created_at)
                FROM rag_chat_messages m
                LEFT JOIN rag_chat_sessions s ON s.session_id = m.session_id
                WHERE s.session_id IS NULL
                GROUP BY m.session_id
                """);
    }

    private void ensureColumn(DatabaseMetaData metadata, String tableName, String columnName, String columnType) throws SQLException {
        if (columnExists(metadata, tableName, columnName)) {
            return;
        }
        jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnType + " NULL");
    }

    private boolean columnExists(DatabaseMetaData metadata, String tableName, String columnName) throws SQLException {
        return hasColumn(metadata, tableName, columnName)
                || hasColumn(metadata, tableName.toUpperCase(), columnName.toUpperCase())
                || hasColumn(metadata, tableName.toLowerCase(), columnName.toLowerCase());
    }

    private boolean hasColumn(DatabaseMetaData metadata, String tableName, String columnName) throws SQLException {
        try (var resultSet = metadata.getColumns(null, null, tableName, columnName)) {
            return resultSet.next();
        }
    }
}
