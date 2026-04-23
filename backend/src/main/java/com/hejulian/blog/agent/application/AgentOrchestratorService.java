package com.hejulian.blog.agent.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hejulian.blog.agent.domain.enums.AgentRole;
import com.hejulian.blog.agent.domain.enums.EventType;
import com.hejulian.blog.agent.domain.enums.MemoryType;
import com.hejulian.blog.agent.domain.enums.SearchScope;
import com.hejulian.blog.agent.domain.enums.TaskStatus;
import com.hejulian.blog.agent.domain.tool.ToolExecutionResult;
import com.hejulian.blog.agent.entity.AgentTask;
import com.hejulian.blog.agent.entity.AgentTaskStep;
import com.hejulian.blog.agent.dto.AgentDtos;
import com.hejulian.blog.agent.mapper.AgentTaskMapper;
import com.hejulian.blog.agent.mapper.AgentTaskStepMapper;
import com.hejulian.blog.common.CacheNames;
import com.hejulian.blog.dto.AdminDtos;
import com.hejulian.blog.entity.RagChatMessage;
import com.hejulian.blog.entity.RagChatSession;
import com.hejulian.blog.exception.BusinessException;
import com.hejulian.blog.mapper.RagChatMessageMapper;
import com.hejulian.blog.mapper.RagChatSessionMapper;
import com.hejulian.blog.rag.application.RagRuntimeContextHolder;
import com.hejulian.blog.rag.infrastructure.client.DashScopeModelGateway;
import com.hejulian.blog.service.AdminBlogService;
import com.hejulian.blog.service.AuthService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AgentOrchestratorService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int DEFAULT_RETRY_COUNT = 1;
    private static final int MAX_AUTO_TAGS = 5;
    private static final int PLANNER_MEMORY_BUDGET = 900;
    private static final int RESEARCH_PLAN_BUDGET = 1200;
    private static final int WRITER_PLAN_BUDGET = 900;
    private static final int WRITER_RESEARCH_BUDGET = 3800;
    private static final int REVIEW_RESEARCH_BUDGET = 3000;
    private static final int REVIEW_DRAFT_BUDGET = 9000;
    private static final int PUBLISH_ARTICLE_BUDGET = 5000;

    private final AgentTaskMapper taskMapper;
    private final AgentTaskStepMapper stepMapper;
    private final AgentToolService toolService;
    private final AgentMemoryService memoryService;
    private final AgentTraceService traceService;
    private final AdminBlogService adminBlogService;
    private final AuthService authService;
    private final RagChatSessionMapper ragChatSessionMapper;
    private final RagChatMessageMapper ragChatMessageMapper;
    private final CacheManager cacheManager;
    private final DashScopeModelGateway modelGateway;

    public AgentOrchestratorService(
            AgentTaskMapper taskMapper,
            AgentTaskStepMapper stepMapper,
            AgentToolService toolService,
            AgentMemoryService memoryService,
            AgentTraceService traceService,
            AdminBlogService adminBlogService,
            AuthService authService,
            RagChatSessionMapper ragChatSessionMapper,
            RagChatMessageMapper ragChatMessageMapper,
            CacheManager cacheManager,
            DashScopeModelGateway modelGateway
    ) {
        this.taskMapper = taskMapper;
        this.stepMapper = stepMapper;
        this.toolService = toolService;
        this.memoryService = memoryService;
        this.traceService = traceService;
        this.adminBlogService = adminBlogService;
        this.authService = authService;
        this.ragChatSessionMapper = ragChatSessionMapper;
        this.ragChatMessageMapper = ragChatMessageMapper;
        this.cacheManager = cacheManager;
        this.modelGateway = modelGateway;
    }

    public void runTask(Long taskId) {
        AgentTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("Task not found");
        }
        if (task.getStatus() == TaskStatus.CANCELLED) {
            return;
        }

        try {
            RagRuntimeContextHolder.RagRuntimeOptions runtime = authService.requireRagRuntimeOptions(task.getUserId());
            if (requiresWebSearch(task.getSearchScope()) && !runtime.webSearchEnabled()) {
                throw new BusinessException("当前所选模型不支持联网搜索，请切换为仅站内或重新配置支持联网搜索的千问模型");
            }
            RagRuntimeContextHolder.set(runtime);
            taskMapper.updateStatus(taskId, TaskStatus.RUNNING, 1, "", "", LocalDateTime.now(), null);
            traceService.recordEvent(taskId, null, AgentRole.PLANNER, EventType.TASK_CREATED, "Agent task started", "ok", null);

            AgentWorkingMemory workingMemory = new AgentWorkingMemory(task.getGoal());
            String plan = runPlanner(task, workingMemory);
            if (stopIfCancelled(taskId, AgentRole.PLANNER)) {
                return;
            }
            String research = runResearch(task, workingMemory);
            if (stopIfCancelled(taskId, AgentRole.RESEARCHER)) {
                return;
            }
            String draft = runWriter(task, workingMemory);
            if (stopIfCancelled(taskId, AgentRole.WRITER)) {
                return;
            }
            String finalText = runReviewer(task, workingMemory);
            if (stopIfCancelled(taskId, AgentRole.REVIEWER)) {
                return;
            }
            int finalStep = 4;

            if (Boolean.TRUE.equals(task.getAllowDraftWrite())) {
                workingMemory.finalArticle = finalText;
                if (stopIfCancelled(taskId, AgentRole.PUBLISHER)) {
                    return;
                }
                finalText = runPublisher(task, workingMemory);
                finalStep = 5;
                if (stopIfCancelled(taskId, AgentRole.PUBLISHER)) {
                    return;
                }
            }

            taskMapper.updateStatus(
                    taskId,
                    TaskStatus.COMPLETED,
                    finalStep,
                    finalText,
                    null,
                    task.getStartedAt(),
                    LocalDateTime.now()
            );
            memoryService.createOrReplaceTaskMemory(task.getUserId(), taskId, finalText, summarize(finalText, 180));
            persistAgentConversation(task, finalText);
            evictPublicCaches();
            traceService.recordEvent(taskId, null, AgentRole.REVIEWER, EventType.TASK_COMPLETED, "Task finished", "ok", null);
        } catch (BusinessException ex) {
            taskMapper.updateStatus(
                    taskId,
                    TaskStatus.FAILED,
                    null,
                    task.getFinalOutputSummary(),
                    ex.getMessage(),
                    task.getStartedAt(),
                    LocalDateTime.now()
            );
            traceService.recordEvent(taskId, null, AgentRole.REVIEWER, EventType.TASK_FAILED, ex.getMessage(), "failed", null);
            throw ex;
        } catch (Exception ex) {
            taskMapper.updateStatus(
                    taskId,
                    TaskStatus.FAILED,
                    null,
                    task.getFinalOutputSummary(),
                    ex.getMessage(),
                    task.getStartedAt(),
                    LocalDateTime.now()
            );
            traceService.recordEvent(taskId, null, AgentRole.REVIEWER, EventType.TASK_FAILED, "Unexpected error", "failed", null);
            throw new BusinessException("Task execution failed", ex);
        } finally {
            RagRuntimeContextHolder.clear();
        }
    }

    public void retryTask(Long taskId, String reason) {
        AgentTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("Task not found");
        }
        traceService.recordEvent(taskId, null, AgentRole.REVIEWER, EventType.TASK_RETRY, summarize(reason, 120), "retry", null);
        taskMapper.updateStatus(taskId, TaskStatus.PENDING, null, null, null, LocalDateTime.now(), null);
        runTask(taskId);
    }

    private String runPlanner(AgentTask task, AgentWorkingMemory workingMemory) {
        AgentTaskStep step = startStep(task, 1, AgentRole.PLANNER, "planner", "Build research plan");
        ObjectNode args = OBJECT_MAPPER.createObjectNode();
        args.put("query", task.getGoal());
        args.put("top", 4);
        ToolExecutionResult memoryResult = toolService.callTool(
                task.getUserId(),
                task.getId(),
                step.getId(),
                task.getSessionId(),
                Boolean.TRUE.equals(task.getAllowDraftWrite()),
                false,
                "get_user_memory_summary",
                toJson(args)
        );
        List<AgentDtos.AgentMemoryResponse> memories = memoryService.recallForTask(task.getUserId(), task.getId(), task.getGoal());
        String memorySummary = memories.stream()
                .map(AgentDtos.AgentMemoryResponse::contentSummary)
                .collect(Collectors.joining(" | "));
        workingMemory.userMemory = summarize(memorySummary, PLANNER_MEMORY_BUDGET);
        String plan = buildPlan(task.getGoal(), workingMemory.userMemory, memoryResult.summary());
        workingMemory.plan = plan;
        workingMemory.planSummary = summarizeForAgent("Planner", plan, RESEARCH_PLAN_BUDGET);
        finishStep(step, plan);
        return plan;
    }

    private String runResearch(AgentTask task, AgentWorkingMemory workingMemory) {
        AgentTaskStep step = startStep(task, 2, AgentRole.RESEARCHER, "research", "Collect evidence with site, knowledge, and web tools");
        ObjectNode args = OBJECT_MAPPER.createObjectNode();
        args.put("query", buildResearchQuery(task));
        args.put("limit", 5);
        String scope = task.getSearchScope() == null ? SearchScope.LOCAL_ONLY.name() : task.getSearchScope().name();
        if (SearchScope.LOCAL_AND_WEB.name().equals(scope)) {
            args.put("scope", "local_and_web");
        } else if (SearchScope.WEB_ONLY.name().equals(scope)) {
            args.put("scope", "web_only");
        } else {
            args.put("scope", "local_only");
        }
        ToolExecutionResult searchResult = null;
        ToolExecutionResult chunkResult = null;
        if (!SearchScope.WEB_ONLY.name().equals(scope)) {
            searchResult = toolService.callTool(
                    task.getUserId(),
                    task.getId(),
                    step.getId(),
                    task.getSessionId(),
                    Boolean.TRUE.equals(task.getAllowDraftWrite()),
                    false,
                    "search_site_posts",
                    toJson(args)
            );
            chunkResult = toolService.callTool(
                    task.getUserId(),
                    task.getId(),
                    step.getId(),
                    task.getSessionId(),
                    Boolean.TRUE.equals(task.getAllowDraftWrite()),
                    false,
                    "search_knowledge_chunks",
                    toJson(args)
            );
        }
        ToolExecutionResult webResult = null;
        if (SearchScope.LOCAL_AND_WEB.name().equals(scope) || SearchScope.WEB_ONLY.name().equals(scope)) {
            ObjectNode webArgs = OBJECT_MAPPER.createObjectNode();
            webArgs.put("query", task.getGoal());
            webResult = toolService.callTool(
                    task.getUserId(),
                    task.getId(),
                    step.getId(),
                    task.getSessionId(),
                    Boolean.TRUE.equals(task.getAllowDraftWrite()),
                    false,
                    "web_search",
                    toJson(webArgs)
            );
        }
        String research = combineForResearch(searchResult, chunkResult, webResult, task.getGoal(), workingMemory.planSummary);
        workingMemory.research = research;
        workingMemory.researchSummary = summarizeForAgent("Researcher", research, WRITER_RESEARCH_BUDGET);
        finishStep(step, research);
        return research;
    }

    private boolean requiresWebSearch(SearchScope searchScope) {
        return SearchScope.LOCAL_AND_WEB.equals(searchScope) || SearchScope.WEB_ONLY.equals(searchScope);
    }

    private String buildResearchQuery(AgentTask task) {
        String title = extractPostTitle(task);
        if (StringUtils.hasText(title) && !"Agent generated post".equals(title)) {
            return title;
        }
        return summarize(task.getGoal(), 120);
    }

    private boolean stopIfCancelled(Long taskId, AgentRole role) {
        AgentTask latest = taskMapper.selectById(taskId);
        if (latest == null || !TaskStatus.CANCELLED.equals(latest.getStatus())) {
            return false;
        }
        traceService.recordEvent(taskId, null, role, EventType.TASK_CANCELLED, "Task cancellation acknowledged", "cancelled", null);
        return true;
    }

    private String runWriter(AgentTask task, AgentWorkingMemory workingMemory) {
        AgentTaskStep step = startStep(task, 3, AgentRole.WRITER, "writer", "Compose draft from plan and research");
        String draft = buildDraft(
                task.getGoal(),
                summarize(workingMemory.planSummary, WRITER_PLAN_BUDGET),
                workingMemory.researchSummary
        );
        workingMemory.draft = draft;
        workingMemory.draftSummary = summarizeForAgent("Writer", draft, REVIEW_DRAFT_BUDGET);
        finishStep(step, draft);
        return draft;
    }

    private String runReviewer(AgentTask task, AgentWorkingMemory workingMemory) {
        AgentTaskStep step = startStep(task, 4, AgentRole.REVIEWER, "reviewer", "Validate draft quality");
        String draft = workingMemory.draft;
        if (!StringUtils.hasText(draft)) {
            throw new BusinessException("Writer produced empty draft");
        }
        if (!StringUtils.hasText(workingMemory.researchSummary)) {
            draft = "No enough source found. " + draft;
        }
        draft = reviewAndReviseDraft(
                task.getGoal(),
                summarize(workingMemory.draftSummary, REVIEW_DRAFT_BUDGET),
                summarize(workingMemory.researchSummary, REVIEW_RESEARCH_BUDGET)
        );
        workingMemory.finalArticle = draft;
        if (draft.contains("N/A")) {
            for (int i = 1; i <= 2; i++) {
                traceService.recordEvent(task.getId(), step.getId(), AgentRole.REVIEWER, EventType.STEP_STARTED, "Reviewer retry attempt " + i, "retry", null);
            }
        }
        finishStep(step, draft);
        return draft;
    }

    private String runPublisher(AgentTask task, AgentWorkingMemory workingMemory) {
        AgentTaskStep step = startStep(task, 5, AgentRole.PUBLISHER, "publisher", "Publish post and save task note");
        String draft = workingMemory.finalArticle;
        ObjectNode args = OBJECT_MAPPER.createObjectNode();
        args.put("content", draft);
        args.put("topicKey", normalizeTopic(task.getGoal()));
        args.put("memoryType", MemoryType.NOTE.name());
        args.put("confidenceScore", 0.88);
        args.put("sourceType", "agent");
        args.put("sourceRefId", task.getId());
        toolService.callTool(
                task.getUserId(),
                task.getId(),
                step.getId(),
                task.getSessionId(),
                true,
                true,
                "save_task_note",
                toJson(args)
        );
        PostMetadata metadata = buildPostMetadata(extractPostTitle(task), summarize(draft, PUBLISH_ARTICLE_BUDGET), draft);
        var post = adminBlogService.savePost(new AdminDtos.PostSaveRequest(
                null,
                metadata.title(),
                null,
                metadata.summary(),
                null,
                draft,
                "PUBLISHED",
                false,
                true,
                metadata.tags()
        ));
        String publishedSummary = draft + "\n\nPublished post: " + post.title() + " (/" + post.slug() + ")";
        finishStep(step, "Published post #" + post.id() + " and saved task note");
        return publishedSummary;
    }

    private String buildPlan(String goal, String memorySummary, String memoryResult) {
        String base = StringUtils.hasText(memorySummary) ? memorySummary : "no memory";
        String plan = modelGateway.generate(
                """
                You are the planner in a controlled blog-writing agent workflow.
                Produce a concise execution plan in Chinese Markdown.
                Include: task intent, target audience, required research questions, writing outline, quality gates, and publish metadata requirements.
                Do not write the article yet.
                """,
                """
                User goal:
                %s

                Relevant memory:
                %s

                Memory tool seed:
                %s
                """.formatted(goal, base, memoryResult),
                0.2D
        );
        if (StringUtils.hasText(plan)) {
            return cleanGeneratedArticle(plan);
        }
        return "Goal: " + summarize(goal, 180) + "\nMemory: " + summarize(base, 240) + "\nPlan: research, draft, review, publish metadata.";
    }

    private String combineForResearch(ToolExecutionResult searchResult, ToolExecutionResult chunkResult, ToolExecutionResult webResult, String detail, String plan) {
        String webSection = webResult == null
                ? "No web search requested or no web result."
                : summarize(webResult.payload(), 2400);
        return """
                User goal:
                %s

                Plan:
                %s

                Site post evidence:
                %s

                Knowledge chunk evidence:
                %s

                Web evidence:
                %s
                """.formatted(
                detail,
                summarize(plan, 1800),
                searchResult == null ? "Skipped because search scope is web only." : summarize(searchResult.payload(), 1800),
                chunkResult == null ? "Skipped because search scope is web only." : summarize(chunkResult.payload(), 1800),
                webSection
        ).trim();
    }

    private String buildDraft(String goal, String plan, String research) {
        String article = modelGateway.generate(
                """
                You are a senior Chinese technical writer for a personal knowledge blog.
                Write only the final article body in Markdown. Do not include planning notes, debug text, tool traces, or meta commentary.
                Follow the user's requested title, length, and publishing intent. If the user asks for more than 2000 Chinese characters, satisfy that length.
                Prefer clear structure, practical examples, beginner-friendly explanations, and concrete comparisons.
                If research contains sources or uncertainty, reflect them carefully without inventing facts.
                Use Chinese unless the user explicitly asks otherwise.
                """,
                """
                User request:
                %s

                Research plan:
                %s

                Research findings:
                %s

                Now write the complete publish-ready article with a strong title, logical headings, examples, and a concise conclusion.
                """.formatted(goal, plan, research),
                0.45D
        );
        if (!StringUtils.hasText(article)) {
            throw new BusinessException("Writer model produced empty article");
        }
        return cleanGeneratedArticle(article);
    }

    private String reviewAndReviseDraft(String goal, String draft, String research) {
        String reviewed = modelGateway.generate(
                """
                You are the reviewer in a controlled blog-writing agent workflow.
                Review and revise the draft directly. Return only the improved publish-ready Markdown article.
                Quality gates:
                - Keep the requested title and length intent.
                - Remove debug text, plan text, tool traces, and "Published post" footers.
                - Improve structure, accuracy, examples, and readability.
                - Do not add unsupported claims beyond the research notes.
                - Preserve Chinese unless the user explicitly asked otherwise.
                """,
                """
                User goal:
                %s

                Research notes:
                %s

                Draft:
                %s
                """.formatted(goal, summarize(research, 5000), draft),
                0.25D
        );
        if (StringUtils.hasText(reviewed)) {
            return cleanGeneratedArticle(reviewed);
        }
        if (draft.length() < 140) {
            return draft + "\n\n[Note] The draft is short, and can be rewritten for better quality.";
        }
        return draft;
    }

    private AgentTaskStep startStep(AgentTask task, int stepIndex, AgentRole role, String stepName, String inputSummary) {
        AgentTaskStep step = new AgentTaskStep();
        step.setTaskId(task.getId());
        step.setStepIndex(stepIndex);
        step.setAgentRole(role);
        step.setStepName(stepName);
        step.setStatus(TaskStatus.RUNNING);
        step.setInputSummary(inputSummary);
        step.setRetryCount(DEFAULT_RETRY_COUNT);
        step.setStartedAt(LocalDateTime.now());
        stepMapper.insert(step);
        traceService.recordEvent(task.getId(), step.getId(), role, EventType.STEP_STARTED, inputSummary, "running", null);
        taskMapper.updateStatus(task.getId(), null, stepIndex, null, null, null, null);
        return step;
    }

    private void finishStep(AgentTaskStep step, String outputSummary) {
        long duration = 0L;
        if (step.getStartedAt() != null) {
            duration = java.time.Duration.between(step.getStartedAt(), LocalDateTime.now()).toMillis();
        }
        stepMapper.updateExecution(
                step.getId(),
                TaskStatus.COMPLETED.name(),
                summarize(outputSummary, 1200),
                step.getRetryCount(),
                duration,
                LocalDateTime.now()
        );
        traceService.recordEvent(
                step.getTaskId(),
                step.getId(),
                step.getAgentRole(),
                EventType.STEP_COMPLETED,
                summarize(outputSummary, 120),
                "completed",
                duration
        );
    }

    private String summarize(String source, int maxLength) {
        if (!StringUtils.hasText(source)) {
            return "";
        }
        String normalized = source.trim().replaceAll("\\s+", " ");
        return normalized.length() > maxLength ? normalized.substring(0, maxLength).trim() + "..." : normalized;
    }

    private String normalizeTopic(String goal) {
        String normalized = StringUtils.hasText(goal) ? goal.trim().toLowerCase() : "task";
        return normalized.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]+", "_").replaceAll("^_+|_+$", "");
    }

    private String extractPostTitle(AgentTask task) {
        String goal = task.getGoal();
        if (StringUtils.hasText(goal)) {
            java.util.regex.Matcher matcher = java.util.regex.Pattern
                    .compile("(?:\\u540d\\u4e3a|\\u6807\\u9898\\u4e3a|\\u9898\\u4e3a)[\\s\\\"\\u201c\\u201d'\\u2018\\u2019\\u300a\\u300b]*([^\\\"\\u201c\\u201d'\\u2018\\u2019\\u300a\\u300b\\uff0c,\\u3002\\uff1b;\\n]+)")
                    .matcher(goal.trim());
            if (matcher.find() && StringUtils.hasText(matcher.group(1))) {
                return summarize(matcher.group(1), 80);
            }
        }
        return StringUtils.hasText(task.getTitle()) ? summarize(task.getTitle(), 80) : "Agent generated post";
    }

    private String cleanGeneratedArticle(String article) {
        String cleaned = article.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("^```[a-zA-Z0-9_-]*\\s*", "");
            cleaned = cleaned.replaceFirst("\\s*```$", "");
        }
        return cleaned.trim();
    }

    private String summarizeForAgent(String role, String content, int budget) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        if (content.length() <= budget) {
            return content.trim();
        }
        String summary = modelGateway.generate(
                """
                Compress the input into role-specific working memory.
                Keep only facts, decisions, constraints, sources, and unresolved issues useful for the next agent.
                Return concise Chinese Markdown bullets. Do not add new facts.
                """,
                """
                Target role:
                %s

                Token budget hint: %d characters.

                Input:
                %s
                """.formatted(role, budget, summarize(content, Math.min(content.length(), budget * 3))),
                0.1D
        );
        if (StringUtils.hasText(summary)) {
            return summarize(cleanGeneratedArticle(summary), budget);
        }
        return summarize(content, budget);
    }

    private PostMetadata buildPostMetadata(String title, String metadataArticleView, String fullArticle) {
        String fallbackTitle = StringUtils.hasText(title) ? title.trim() : "Agent generated post";
        String fallbackSummary = summarize(stripMarkdown(removePublishedFooter(fullArticle)), 180);
        List<String> fallbackTags = inferFallbackTags(fallbackTitle, fullArticle);

        String metadataJson = modelGateway.generate(
                """
                You generate concise publishing metadata for a Chinese blog post.
                Return only compact JSON. Do not wrap it in Markdown.
                Required shape: {"summary":"80-160 Chinese characters, no title repetition, no code, no meta text","tags":["tag1","tag2","tag3"]}
                Tags should be topical keywords, 3 to 5 items, no generic words like Agent unless the article is actually about agents.
                """,
                """
                Title: %s

                Article:
                %s
                """.formatted(fallbackTitle, metadataArticleView),
                0.2D
        );

        if (!StringUtils.hasText(metadataJson)) {
            return new PostMetadata(fallbackTitle, fallbackSummary, fallbackTags);
        }

        try {
            String normalizedJson = metadataJson.trim()
                    .replaceFirst("^```(?:json)?\\s*", "")
                    .replaceFirst("\\s*```$", "");
            var node = OBJECT_MAPPER.readTree(normalizedJson);
            String summary = summarize(stripMarkdown(node.path("summary").asText(fallbackSummary)), 180);
            List<String> tags = new ArrayList<>();
            if (node.path("tags").isArray()) {
                for (var item : node.path("tags")) {
                    String tag = normalizeGeneratedTag(item.asText());
                    if (StringUtils.hasText(tag) && tags.size() < MAX_AUTO_TAGS) {
                        tags.add(tag);
                    }
                }
            }
            if (tags.isEmpty()) {
                tags = fallbackTags;
            }
            return new PostMetadata(fallbackTitle, StringUtils.hasText(summary) ? summary : fallbackSummary, tags);
        } catch (Exception ex) {
            return new PostMetadata(fallbackTitle, fallbackSummary, fallbackTags);
        }
    }

    private List<String> inferFallbackTags(String title, String article) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        String source = (title + " " + stripMarkdown(article)).toLowerCase();
        addTagIfContains(tags, source, "agent", "Agent");
        addTagIfContains(tags, source, "bfs", "BFS");
        addTagIfContains(tags, source, "dfs", "DFS");
        addTagIfContains(tags, source, "rag", "RAG");
        addTagIfContains(tags, source, "llm", "LLM");
        addTagIfContains(tags, source, "\u7b97\u6cd5", "\u7b97\u6cd5");
        addTagIfContains(tags, source, "\u67b6\u6784", "\u67b6\u6784");
        addTagIfContains(tags, source, "\u641c\u7d22", "\u641c\u7d22");
        if (tags.isEmpty()) {
            tags.add("\u6280\u672f\u6587\u7ae0");
        }
        return tags.stream().limit(MAX_AUTO_TAGS).toList();
    }

    private void addTagIfContains(LinkedHashSet<String> tags, String source, String keyword, String tag) {
        if (source.contains(keyword.toLowerCase()) && tags.size() < MAX_AUTO_TAGS) {
            tags.add(tag);
        }
    }

    private String normalizeGeneratedTag(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim()
                .replaceAll("^#+", "")
                .replaceAll("[`'\"\\[\\](){}]", "")
                .replaceAll("\\s{2,}", " ")
                .replaceAll("^[\\p{Punct}\\s]+|[\\p{Punct}\\s]+$", "");
        return normalized.isBlank() ? null : summarize(normalized, 24);
    }

    private String removePublishedFooter(String source) {
        if (!StringUtils.hasText(source)) {
            return "";
        }
        return source.replaceAll("\\n\\nPublished post:.+$", "").trim();
    }

    private record PostMetadata(String title, String summary, List<String> tags) {
    }

    private static final class AgentWorkingMemory {
        private String userMemory = "";
        private String plan = "";
        private String planSummary = "";
        private String research = "";
        private String researchSummary = "";
        private String draft = "";
        private String draftSummary = "";
        private String finalArticle = "";

        private AgentWorkingMemory(String goal) {
        }
    }

    private String stripMarkdown(String source) {
        if (!StringUtils.hasText(source)) {
            return "";
        }
        return source.replaceAll("[#>*_`\\-\\[\\]()]", " ").replaceAll("\\s+", " ").trim();
    }

    private void persistAgentConversation(AgentTask task, String finalText) {
        if (!StringUtils.hasText(task.getSessionId())) {
            return;
        }

        RagChatMessage userMessage = new RagChatMessage();
        userMessage.setSessionId(task.getSessionId());
        userMessage.setUserId(task.getUserId());
        userMessage.setRole("user");
        userMessage.setContent(task.getGoal());
        userMessage.setAnswerMode(null);
        userMessage.setCitationsJson("[]");
        userMessage.setSourcesJson("[]");
        userMessage.setVariantsJson("[]");
        ragChatMessageMapper.insert(userMessage);

        RagChatMessage assistantMessage = new RagChatMessage();
        assistantMessage.setSessionId(task.getSessionId());
        assistantMessage.setUserId(task.getUserId());
        assistantMessage.setRole("assistant");
        assistantMessage.setContent(finalText);
        assistantMessage.setAnswerMode("agent");
        assistantMessage.setCitationsJson("[]");
        assistantMessage.setSourcesJson("[]");
        assistantMessage.setVariantsJson("[]");
        ragChatMessageMapper.insert(assistantMessage);

        int messageCount = ragChatMessageMapper.selectBySessionId(task.getUserId(), task.getSessionId()).size();
        RagChatSession existing = ragChatSessionMapper.selectBySessionId(task.getUserId(), task.getSessionId());
        LocalDateTime now = LocalDateTime.now();
        String title = summarize(task.getGoal(), 80);
        String preview = summarize(task.getGoal(), 240);
        if (existing == null) {
            RagChatSession session = new RagChatSession();
            session.setSessionId(task.getSessionId());
            session.setUserId(task.getUserId());
            session.setTitle(title);
            session.setPreview(preview);
            session.setMessageCount(messageCount);
            session.setManualTitle(Boolean.FALSE);
            session.setDeleted(Boolean.FALSE);
            session.setCreatedAt(now);
            session.setUpdatedAt(now);
            ragChatSessionMapper.insert(session);
        } else {
            ragChatSessionMapper.updateLifecycle(
                    task.getUserId(),
                    task.getSessionId(),
                    Boolean.TRUE.equals(existing.getManualTitle()) ? existing.getTitle() : title,
                    preview,
                    messageCount,
                    now,
                    false
            );
        }

        evictConversationCaches(task.getUserId(), task.getSessionId());
    }

    private void evictPublicCaches() {
        clearCache(CacheNames.SITE_HOME);
        clearCache(CacheNames.PUBLIC_POST_LIST);
    }

    private void evictConversationCaches(Long userId, String sessionId) {
        Cache historyCache = cacheManager.getCache(CacheNames.RAG_HISTORY);
        if (historyCache != null && StringUtils.hasText(sessionId)) {
            historyCache.evict(userId + ":" + sessionId.trim());
        }
        Cache sessionCache = cacheManager.getCache(CacheNames.RAG_SESSION_LIST);
        if (sessionCache != null) {
            sessionCache.evict(userId + ":" + Boolean.TRUE);
            sessionCache.evict(userId + ":" + Boolean.FALSE);
        }
    }

    private void clearCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }

    private String toJson(ObjectNode node) {
        try {
            return OBJECT_MAPPER.writeValueAsString(node);
        } catch (Exception ex) {
            return "{}";
        }
    }
}

