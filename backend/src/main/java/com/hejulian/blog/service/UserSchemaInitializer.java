package com.hejulian.blog.service;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Order(1)
public class UserSchemaInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    @Override
    public void run(String... args) {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            ensureColumn(metadata, "user_accounts", "email", "VARCHAR(160)");
            ensureColumn(metadata, "user_accounts", "qwen_api_key", "VARCHAR(512)");
            ensureColumn(metadata, "user_accounts", "qwen_chat_model", "VARCHAR(80)");
            ensureColumn(metadata, "user_accounts", "qwen_web_search_enabled", "BOOLEAN");
            ensureColumn(metadata, "comments", "user_id", "BIGINT");
            jdbcTemplate.update(
                    "UPDATE user_accounts SET email = ? WHERE username = ? AND (email IS NULL OR email = '')",
                    "admin@example.com",
                    "admin"
            );
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to initialize user schema", ex);
        }
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
