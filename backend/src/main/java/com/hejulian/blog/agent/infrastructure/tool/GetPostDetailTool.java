package com.hejulian.blog.agent.infrastructure.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hejulian.blog.agent.domain.enums.ToolPermissionLevel;
import com.hejulian.blog.agent.domain.tool.AgentTool;
import com.hejulian.blog.agent.domain.tool.AgentToolContext;
import com.hejulian.blog.agent.domain.tool.ToolExecutionResult;
import com.hejulian.blog.exception.BusinessException;
import com.hejulian.blog.mapper.PostMapper;
import com.hejulian.blog.entity.Post;
import org.springframework.stereotype.Component;

@Component
public class GetPostDetailTool implements AgentTool {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final PostMapper postMapper;

    public GetPostDetailTool(PostMapper postMapper) {
        this.postMapper = postMapper;
    }

    @Override
    public String name() {
        return "get_post_detail";
    }

    @Override
    public String description() {
        return "Get one published post detail by postId";
    }

    @Override
    public JsonNode inputSchema() {
        ObjectNode schema = OBJECT_MAPPER.createObjectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("postId").put("type", "integer");
        return schema;
    }

    @Override
    public ToolPermissionLevel permissionLevel() {
        return ToolPermissionLevel.READ;
    }

    @Override
    public void validateInput(JsonNode args) {
        if (args == null || !args.hasNonNull("postId") || args.get("postId").asLong() <= 0) {
            throw new BusinessException("postId is required");
        }
    }

    @Override
    public ToolExecutionResult execute(AgentToolContext context, JsonNode args) {
        long postId = args.get("postId").asLong();
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException("Post not found");
        }
        String summary = "%s|%s|%s".formatted(post.getTitle(), post.getSummary(), post.getStatus());
        return new ToolExecutionResult(true, "Post detail loaded", summary);
    }
}
