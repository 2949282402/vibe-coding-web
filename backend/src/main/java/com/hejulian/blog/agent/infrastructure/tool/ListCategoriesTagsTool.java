package com.hejulian.blog.agent.infrastructure.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hejulian.blog.agent.domain.enums.ToolPermissionLevel;
import com.hejulian.blog.agent.domain.tool.AgentTool;
import com.hejulian.blog.agent.domain.tool.AgentToolContext;
import com.hejulian.blog.agent.domain.tool.ToolExecutionResult;
import com.hejulian.blog.exception.BusinessException;
import com.hejulian.blog.mapper.CategoryMapper;
import com.hejulian.blog.mapper.TagMapper;
import org.springframework.stereotype.Component;

@Component
public class ListCategoriesTagsTool implements AgentTool {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;

    public ListCategoriesTagsTool(CategoryMapper categoryMapper, TagMapper tagMapper) {
        this.categoryMapper = categoryMapper;
        this.tagMapper = tagMapper;
    }

    @Override
    public String name() {
        return "list_categories_tags";
    }

    @Override
    public String description() {
        return "List site categories and tags";
    }

    @Override
    public JsonNode inputSchema() {
        ObjectNode schema = OBJECT_MAPPER.createObjectNode();
        schema.put("type", "object");
        schema.putObject("properties");
        return schema;
    }

    @Override
    public ToolPermissionLevel permissionLevel() {
        return ToolPermissionLevel.READ;
    }

    @Override
    public void validateInput(JsonNode args) {
        if (args == null) {
            return;
        }
    }

    @Override
    public ToolExecutionResult execute(AgentToolContext context, JsonNode args) {
        long categoryCount = categoryMapper.countAll();
        long tagCount = tagMapper.countAll();
        String categories = categoryMapper.selectAllOrderByName().stream()
                .map(category -> category.getName())
                .reduce("", (acc, name) -> acc.isEmpty() ? name : acc + "," + name);
        String tags = tagMapper.selectAllOrderByName().stream()
                .map(tag -> tag.getName())
                .reduce("", (acc, name) -> acc.isEmpty() ? name : acc + "," + name);
        return new ToolExecutionResult(
                true,
                "Found categories=%d tags=%d".formatted(categoryCount, tagCount),
                "categories: " + categories + "; tags: " + tags
        );
    }
}
