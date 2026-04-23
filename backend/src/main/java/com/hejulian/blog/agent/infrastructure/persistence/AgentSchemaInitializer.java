package com.hejulian.blog.agent.infrastructure.persistence;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;

@Component
@RequiredArgsConstructor
@Order(2)
@ConditionalOnProperty(prefix = "agent.schema", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AgentSchemaInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    @Override
    public void run(String... args) {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            String productName = metadata.getDatabaseProductName();
            boolean h2 = isH2(productName);

            jdbcTemplate.execute(buildCreateTaskTableSql(h2));
            jdbcTemplate.execute(buildCreateTaskStepTableSql(h2));
            jdbcTemplate.execute(buildCreateTaskEventTableSql(h2));
            jdbcTemplate.execute(buildCreateToolCallTableSql(h2));
            jdbcTemplate.execute(buildCreateMemoryTableSql(h2));
            jdbcTemplate.execute(buildCreateMemoryHitTableSql(h2));
            jdbcTemplate.execute(buildCreateEvalRecordTableSql(h2));

            ensureColumn(metadata, "agent_memory", "is_deleted", "BOOLEAN");
            ensureColumn(metadata, "agent_memory_hit", "used_in_step", "VARCHAR(64)");
            ensureColumn(metadata, "agent_tool_call", "success", "BOOLEAN");
            ensureColumn(metadata, "agent_task", "error_message", "VARCHAR(1000)");
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to initialize agent schema", ex);
        }
    }

    private String buildCreateTaskTableSql(boolean h2) {
        String timestampType = h2 ? "TIMESTAMP" : "DATETIME";
        String textType = h2 ? "CLOB" : "LONGTEXT";
        return """
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
                    allow_draft_write BOOLEAN NOT NULL DEFAULT FALSE,
                    current_step INT NOT NULL DEFAULT 0,
                    final_output_summary %s NULL,
                    error_message VARCHAR(1000) NULL,
                    started_at %s NULL,
                    completed_at %s NULL,
                    created_at %s NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at %s NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    KEY idx_agent_task_user_id (user_id),
                    KEY idx_agent_task_user_status (user_id, status),
                    KEY idx_agent_task_created_at (created_at),
                    CONSTRAINT fk_agent_task_user FOREIGN KEY (user_id) REFERENCES user_accounts (id) ON DELETE CASCADE
                )
                """.formatted(textType, timestampType, timestampType, timestampType, timestampType);
    }

    private String buildCreateTaskStepTableSql(boolean h2) {
        String timestampType = h2 ? "TIMESTAMP" : "DATETIME";
        String textType = h2 ? "CLOB" : "LONGTEXT";
        return """
                CREATE TABLE IF NOT EXISTS agent_task_step (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    task_id BIGINT NOT NULL,
                    step_index INT NOT NULL,
                    agent_role VARCHAR(32) NOT NULL,
                    step_name VARCHAR(120) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    input_summary VARCHAR(500) NULL,
                    output_summary %s NULL,
                    retry_count INT NULL DEFAULT 0,
                    latency_ms BIGINT NULL,
                    started_at %s NULL,
                    completed_at %s NULL,
                    created_at %s NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    KEY idx_agent_task_step_task_id (task_id, step_index),
                    CONSTRAINT fk_agent_task_step_task FOREIGN KEY (task_id) REFERENCES agent_task (id) ON DELETE CASCADE
                )
                """.formatted(textType, timestampType, timestampType, timestampType);
    }

    private String buildCreateTaskEventTableSql(boolean h2) {
        String timestampType = h2 ? "TIMESTAMP" : "DATETIME";
        String textType = h2 ? "CLOB" : "LONGTEXT";
        return """
                CREATE TABLE IF NOT EXISTS agent_task_event (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    task_id BIGINT NOT NULL,
                    step_id BIGINT NULL,
                    event_type VARCHAR(32) NOT NULL,
                    agent_role VARCHAR(32) NOT NULL,
                    payload_json %s NULL,
                    payload_summary VARCHAR(500) NULL,
                    status VARCHAR(64) NULL,
                    latency_ms BIGINT NULL,
                    created_at %s NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    KEY idx_agent_task_event_task_id (task_id, id),
                    CONSTRAINT fk_agent_task_event_task FOREIGN KEY (task_id) REFERENCES agent_task (id) ON DELETE CASCADE,
                    CONSTRAINT fk_agent_task_event_step FOREIGN KEY (step_id) REFERENCES agent_task_step (id) ON DELETE SET NULL
                )
                """.formatted(textType, timestampType);
    }

    private String buildCreateToolCallTableSql(boolean h2) {
        String timestampType = h2 ? "TIMESTAMP" : "DATETIME";
        String textType = h2 ? "CLOB" : "LONGTEXT";
        return """
                CREATE TABLE IF NOT EXISTS agent_tool_call (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    task_id BIGINT NOT NULL,
                    step_id BIGINT NULL,
                    tool_name VARCHAR(80) NOT NULL,
                    permission_level VARCHAR(16) NOT NULL,
                    request_json %s NULL,
                    response_summary VARCHAR(300) NULL,
                    success BOOLEAN NULL,
                    error_message VARCHAR(1000) NULL,
                    latency_ms BIGINT NULL,
                    created_at %s NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    KEY idx_agent_tool_call_task_id (task_id, id),
                    CONSTRAINT fk_agent_tool_call_task FOREIGN KEY (task_id) REFERENCES agent_task (id) ON DELETE CASCADE,
                    CONSTRAINT fk_agent_tool_call_step FOREIGN KEY (step_id) REFERENCES agent_task_step (id) ON DELETE SET NULL
                )
                """.formatted(textType, timestampType);
    }

    private String buildCreateMemoryTableSql(boolean h2) {
        String timestampType = h2 ? "TIMESTAMP" : "DATETIME";
        String textType = h2 ? "CLOB" : "LONGTEXT";
        return """
                CREATE TABLE IF NOT EXISTS agent_memory (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    user_id BIGINT NOT NULL,
                    memory_scope VARCHAR(32) NOT NULL,
                    topic_key VARCHAR(200) NOT NULL,
                    memory_type VARCHAR(32) NOT NULL,
                    content %s NOT NULL,
                    content_summary VARCHAR(500) NULL,
                    confidence_score DECIMAL(5,4) NULL,
                    source_type VARCHAR(64) NULL,
                    source_ref_id BIGINT NULL,
                    is_pinned BOOLEAN NOT NULL DEFAULT FALSE,
                    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
                    last_hit_at %s NULL,
                    created_at %s NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at %s NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    KEY idx_agent_memory_user_id_scope (user_id, memory_scope),
                    KEY idx_agent_memory_user_topic (user_id, topic_key),
                    CONSTRAINT fk_agent_memory_user FOREIGN KEY (user_id) REFERENCES user_accounts (id) ON DELETE CASCADE
                )
                """.formatted(textType, timestampType, timestampType, timestampType);
    }

    private String buildCreateMemoryHitTableSql(boolean h2) {
        String timestampType = h2 ? "TIMESTAMP" : "DATETIME";
        return """
                CREATE TABLE IF NOT EXISTS agent_memory_hit (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    task_id BIGINT NOT NULL,
                    memory_id BIGINT NOT NULL,
                    hit_reason VARCHAR(255) NULL,
                    used_in_step VARCHAR(64) NULL,
                    created_at %s NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    KEY idx_agent_memory_hit_task_id (task_id),
                    KEY idx_agent_memory_hit_memory_id (memory_id),
                    CONSTRAINT fk_agent_memory_hit_task FOREIGN KEY (task_id) REFERENCES agent_task (id) ON DELETE CASCADE,
                    CONSTRAINT fk_agent_memory_hit_memory FOREIGN KEY (memory_id) REFERENCES agent_memory (id) ON DELETE CASCADE
                )
                """.formatted(timestampType);
    }

    private String buildCreateEvalRecordTableSql(boolean h2) {
        String timestampType = h2 ? "TIMESTAMP" : "DATETIME";
        String textType = h2 ? "CLOB" : "TEXT";
        return """
                CREATE TABLE IF NOT EXISTS agent_eval_record (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    task_id BIGINT NOT NULL,
                    user_id BIGINT NOT NULL,
                    score_overall INT NULL,
                    score_grounding INT NULL,
                    score_helpfulness INT NULL,
                    score_style INT NULL,
                    issue_types VARCHAR(500) NULL,
                    feedback_note %s NULL,
                    created_at %s NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    KEY idx_agent_eval_task_id (task_id),
                    KEY idx_agent_eval_user_id (user_id),
                    CONSTRAINT fk_agent_eval_record_task FOREIGN KEY (task_id) REFERENCES agent_task (id) ON DELETE CASCADE,
                    CONSTRAINT fk_agent_eval_record_user FOREIGN KEY (user_id) REFERENCES user_accounts (id) ON DELETE CASCADE
                )
                """.formatted(textType, timestampType);
    }

    private boolean isH2(String productName) {
        return productName != null && productName.toLowerCase().contains("h2");
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
