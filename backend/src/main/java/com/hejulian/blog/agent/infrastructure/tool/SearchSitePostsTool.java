package com.hejulian.blog.agent.infrastructure.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hejulian.blog.agent.domain.enums.ToolPermissionLevel;
import com.hejulian.blog.agent.domain.tool.AgentTool;
import com.hejulian.blog.agent.domain.tool.AgentToolContext;
import com.hejulian.blog.agent.domain.tool.ToolExecutionResult;
import com.hejulian.blog.exception.BusinessException;
import com.hejulian.blog.mapper.PostMapper;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class SearchSitePostsTool implements AgentTool {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final PostMapper postMapper;

    public SearchSitePostsTool(PostMapper postMapper) {
        this.postMapper = postMapper;
    }

    @Override
    public String name() {
        return "search_site_posts";
    }

    @Override
    public String description() {
        return "Search published posts in the current site by keyword";
    }

    @Override
    public JsonNode inputSchema() {
        ObjectNode schema = OBJECT_MAPPER.createObjectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("query").put("type", "string");
        properties.putObject("limit").put("type", "integer");
        return schema;
    }

    @Override
    public ToolPermissionLevel permissionLevel() {
        return ToolPermissionLevel.READ;
    }

    @Override
    public void validateInput(JsonNode args) {
        if (args == null || !args.hasNonNull("query") || args.get("query").asText().trim().isBlank()) {
            throw new BusinessException("query is required");
        }
    }

    @Override
    public ToolExecutionResult execute(AgentToolContext context, JsonNode args) {
        String query = args.get("query").asText().trim();
        int limit = args.hasNonNull("limit") ? Math.max(1, Math.min(args.get("limit").asInt(), 10)) : 5;
        var posts = postMapper.selectPublicPosts(query, null, null, 0, limit);
        String payload = posts.stream()
                .map(post -> "#%d %s".formatted(post.getId(), post.getTitle()))
                .collect(Collectors.joining(" | "));
        return new ToolExecutionResult(true, "Found %d posts".formatted(posts.size()), payload);
    }
}
