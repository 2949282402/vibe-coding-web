package com.hejulian.blog.rag.domain.service;

import com.hejulian.blog.dto.RagDtos;
import com.hejulian.blog.rag.domain.model.ChatHistoryMessage;
import com.hejulian.blog.rag.domain.model.ScoredChunk;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RagPromptService {

    private final KnowledgeTextProcessor textProcessor;

    public RagPromptService(KnowledgeTextProcessor textProcessor) {
        this.textProcessor = textProcessor;
    }

    public String buildStrictRetrievalAnswer(String question, List<ScoredChunk> rankedChunks) {
        boolean chinese = textProcessor.containsChinese(question);
        List<ScoredChunk> topChunks = rankedChunks.stream().limit(3).toList();
        StringBuilder builder = new StringBuilder();

        if (chinese) {
            builder.append("根据当前检索结果，可以确认以下内容：\n\n");
            for (int index = 0; index < topChunks.size(); index++) {
                ScoredChunk chunk = topChunks.get(index);
                builder.append(index + 1)
                        .append(". ")
                        .append(textProcessor.summarizeExcerpt(chunk.chunk().content()))
                        .append(" [")
                        .append(index + 1)
                        .append("]\n");
            }
            builder.append("\n如果需要更完整的方案，我可以继续基于这些引用片段展开说明。");
        } else {
            builder.append("Based on the retrieved evidence, the following points can be confirmed:\n\n");
            for (int index = 0; index < topChunks.size(); index++) {
                ScoredChunk chunk = topChunks.get(index);
                builder.append(index + 1)
                        .append(". ")
                        .append(textProcessor.summarizeExcerpt(chunk.chunk().content()))
                        .append(" [")
                        .append(index + 1)
                        .append("]\n");
            }
            builder.append("\nI can expand this further while keeping the same citation mapping if needed.");
        }

        return builder.toString().trim();
    }

    public String buildSystemPrompt(String question) {
        if (textProcessor.containsChinese(question)) {
            return """
                    你是一个个人技术博客系统的 RAG 助手。
                    你只能基于提供的文章片段作答，不得编造上下文中不存在的信息。
                    必须严格引用，任何有事实判断的段落都必须包含引用标记，格式只能使用 [1]、[2] 这类编号。
                    不允许输出未出现在上下文中的引用编号，不允许省略引用。
                    如果上下文不足以支持完整回答，请明确说明“当前知识库不足以完整回答”，并仍然给出已有结论及其引用。
                    回答要简洁、准确，优先从工程实现角度组织内容。
                    """;
        }
        return """
                You are a retrieval-augmented assistant for a personal engineering blog.
                Answer only from the supplied article excerpts.
                Every factual paragraph must contain citation markers in the form [1], [2], and you may only cite the provided context ids.
                Do not produce uncited factual claims or unsupported citation numbers.
                If the retrieved context is insufficient, say so explicitly and keep all supported statements cited.
                """;
    }

    public String buildUserPrompt(String question, List<ScoredChunk> rankedChunks, List<ChatHistoryMessage> historyWindow) {
        StringBuilder builder = new StringBuilder();
        builder.append("Question:\n").append(question.trim()).append("\n\n");

        if (!historyWindow.isEmpty()) {
            builder.append("Conversation History:\n");
            for (ChatHistoryMessage message : historyWindow) {
                builder.append("- ")
                        .append("assistant".equalsIgnoreCase(message.role()) ? "Assistant" : "User")
                        .append(": ")
                        .append(stripCitationMarkers(message.content()))
                        .append("\n");
            }
            builder.append("\n");
        }

        builder.append("Context:\n");
        for (int index = 0; index < rankedChunks.size(); index++) {
            ScoredChunk chunk = rankedChunks.get(index);
            builder.append("[")
                    .append(index + 1)
                    .append("] ")
                    .append(chunk.chunk().postTitle())
                    .append(" (slug: ")
                    .append(chunk.chunk().postSlug())
                    .append(")\n")
                    .append(chunk.chunk().content())
                    .append("\n\n");
        }
        builder.append("Output requirement:\n")
                .append("- Use only the context above.\n")
                .append("- Every factual paragraph must include citations like [1] or [1][2].\n")
                .append("- Do not mention any citation id that is not listed above.\n");
        return builder.toString();
    }

    public List<String> buildFollowUpQuestions(String question, List<RagDtos.Source> sources) {
        boolean chinese = textProcessor.containsChinese(question);
        LinkedHashSet<String> suggestions = new LinkedHashSet<>();
        for (RagDtos.Source source : sources) {
            if (chinese) {
                suggestions.add("展开说明《" + source.title() + "》里的关键实现思路");
                suggestions.add("基于《" + source.title() + "》给出落地步骤");
            } else {
                suggestions.add("Expand the key approach in \"" + source.title() + "\"");
                suggestions.add("Turn \"" + source.title() + "\" into an actionable rollout plan");
            }
            if (suggestions.size() >= 3) {
                break;
            }
        }
        if (suggestions.isEmpty()) {
            suggestions.addAll(localizedFallbackSuggestions(question));
        }
        return suggestions.stream().limit(3).toList();
    }

    public List<String> localizedFallbackSuggestions(String question) {
        if (textProcessor.containsChinese(question)) {
            return List.of(
                    "总结与这个问题最相关的文章内容",
                    "把实现过程拆成步骤说明",
                    "给出适合当前博客项目的下一步方案"
            );
        }
        return List.of(
                "Summarize the posts most related to this topic",
                "Explain the implementation as a sequence of steps",
                "Propose an approach suitable for this blog project"
        );
    }

    public String localizedNoDataAnswer(String question) {
        if (textProcessor.containsChinese(question)) {
            return "当前知识库还没有已发布文章，因此暂时无法生成带引用的 RAG 回答。";
        }
        return "The knowledge base does not contain any published posts yet, so no cited RAG answer can be generated.";
    }

    public String localizedNoMatchAnswer(String question) {
        if (textProcessor.containsChinese(question)) {
            return "当前知识库里没有召回到足够相关的内容。你可以换一个更具体的问题，或者先补充相关文章。";
        }
        return "I could not find enough relevant excerpts in the current knowledge base. Try a more specific query or add more related blog content.";
    }

    private String stripCitationMarkers(String content) {
        return content == null ? "" : content.replaceAll("\\[(\\d+)]", "").trim();
    }
}
