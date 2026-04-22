package com.hejulian.blog.rag.domain.service;

import com.hejulian.blog.dto.RagDtos;
import com.hejulian.blog.rag.domain.model.ChatHistoryMessage;
import com.hejulian.blog.rag.domain.model.RagEvidence;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RagPromptService {

    private final KnowledgeTextProcessor textProcessor;

    public RagPromptService(KnowledgeTextProcessor textProcessor) {
        this.textProcessor = textProcessor;
    }

    public String buildStrictRetrievalAnswer(String question, List<RagEvidence> evidences) {
        boolean chinese = textProcessor.containsChinese(question);
        List<RagEvidence> topEvidence = evidences.stream().limit(3).toList();
        StringBuilder builder = new StringBuilder();

        if (chinese) {
            builder.append("\u57fa\u4e8e\u5df2\u68c0\u7d22\u5230\u7684\u8bc1\u636e\uff0c\u76ee\u524d\u53ef\u4ee5\u786e\u8ba4\uff1a\n\n");
            for (int index = 0; index < topEvidence.size(); index++) {
                RagEvidence evidence = topEvidence.get(index);
                builder.append(index + 1)
                        .append(". ")
                        .append(summarizeEvidence(evidence))
                        .append(" [")
                        .append(evidence.citationIndex())
                        .append("]\n");
            }
            builder.append("\n\u5982\u679c\u4f60\u9700\u8981\uff0c\u6211\u53ef\u4ee5\u7ee7\u7eed\u5c55\u5f00\u7ec6\u8282\uff0c\u5e76\u4fdd\u6301\u540c\u4e00\u7ec4\u5f15\u7528\u7f16\u53f7\u3002");
        } else {
            builder.append("Based on the retrieved evidence, the following points can be confirmed:\n\n");
            for (int index = 0; index < topEvidence.size(); index++) {
                RagEvidence evidence = topEvidence.get(index);
                builder.append(index + 1)
                        .append(". ")
                        .append(summarizeEvidence(evidence))
                        .append(" [")
                        .append(evidence.citationIndex())
                        .append("]\n");
            }
            builder.append("\nI can expand this further while keeping the same citation mapping if needed.");
        }

        return builder.toString().trim();
    }

    public String buildSystemPrompt(String question) {
        return """
                你是个人工程博客的检索增强助手。
                你必须先判断用户是在提问检索问题，还是只是在闲聊、打招呼、客套对话。
                如果用户是在闲聊、打招呼，或者问题本身没有明确的检索需求，必须只回复“暂时没有参考资料”。
                你只能依据给定的参考资料和用户问题回答，不要补充任何未被参考资料直接支持的事实、推断、常识或延伸结论。
                如果参考资料为空，或者参考资料与用户问题不相关，必须只回复“暂时没有参考资料”。
                每一段事实性陈述都必须带上引用标记，例如 [1]、[2]，且只能引用已提供的编号。
                不要编造来源，不要输出没有引用支撑的结论，不要根据训练知识自行补全答案。
                """;
    }

    public String buildWebSearchSystemPrompt(String question) {
        return """
                你是个人工程博客的检索增强助手。
                你必须先判断用户是在提问检索问题，还是只是在闲聊、打招呼、客套对话。
                如果用户是在闲聊、打招呼，或者问题本身没有明确的检索需求，必须只回复“暂时没有参考资料”。
                请严格依据我提供的参考资料和用户问题作答。
                可以参考站内知识片段，也可以参考联网搜索结果，但都必须与用户问题直接相关。
                如果参考资料为空，或者参考资料与用户问题不相关，必须只回复“暂时没有参考资料”。
                对参考资料支持的内容，必须保留或补充 [1]、[2] 这样的引用标记。
                不要编造来源，不要输出没有引用支撑的结论，不要根据训练知识自行补全答案。
                """;
    }

    public String buildUserPrompt(String question, List<RagEvidence> evidences, List<ChatHistoryMessage> historyWindow) {
        boolean chinese = textProcessor.containsChinese(question);
        StringBuilder builder = new StringBuilder();

        builder.append("问题：\n")
                .append(question.trim())
                .append("\n\n");

        if (!historyWindow.isEmpty()) {
            builder.append("最近对话：\n");
            for (ChatHistoryMessage message : historyWindow) {
                builder.append("- ")
                        .append(localizedRole(message.role(), chinese))
                        .append(": ")
                        .append(stripCitationMarkers(message.content()))
                        .append("\n");
            }
            builder.append("\n");
        }

        builder.append("证据上下文：\n");
        for (RagEvidence evidence : evidences) {
            builder.append("[")
                    .append(evidence.citationIndex())
                    .append("] ")
                    .append(evidence.title())
                    .append("\n")
                    .append(describeEvidence(evidence, chinese))
                    .append("\n")
                    .append(evidence.content())
                    .append("\n\n");
        }

        builder.append("输出要求：\n");
        builder.append("- 只能使用上面的证据回答。\n")
                .append("- 必须先判断用户是在提问检索问题，还是只是在闲聊、打招呼、客套对话；如果是闲聊或打招呼，只回复“暂时没有参考资料”。\n")
                .append("- 必须先判断参考资料是否能直接回答用户问题；如果不能直接回答，或参考资料不相关，只回复“暂时没有参考资料”。\n")
                .append("- 每一段事实性内容都必须包含 [1] 或 [1][2] 这样的引用。\n")
                .append("- 不要引用未出现过的编号。\n")
                .append("- 不要使用参考资料之外的常识、经验或训练知识补充答案。\n");
        return builder.toString();
    }

    public String buildWebSearchUserPrompt(String question, List<RagEvidence> localEvidences, List<ChatHistoryMessage> historyWindow) {
        boolean chinese = textProcessor.containsChinese(question);
        StringBuilder builder = new StringBuilder();

        builder.append("用户问题：\n")
                .append(question.trim())
                .append("\n\n");

        if (!historyWindow.isEmpty()) {
            builder.append("最近对话：\n");
            for (ChatHistoryMessage message : historyWindow) {
                builder.append("- ")
                        .append(localizedRole(message.role(), chinese))
                        .append(": ")
                        .append(stripCitationMarkers(message.content()))
                        .append("\n");
            }
            builder.append("\n");
        }

        if (localEvidences.isEmpty()) {
            builder.append("当前没有足够的站内知识片段，你需要重点使用联网搜索补充答案。\n\n");
        } else {
            builder.append("站内知识片段：\n");
            for (RagEvidence evidence : localEvidences) {
                builder.append("[")
                        .append(evidence.citationIndex())
                        .append("] ")
                        .append(evidence.title())
                        .append("\n")
                        .append(textProcessor.summarizeExcerpt(evidence.content()))
                        .append("\n\n");
            }
        }

        builder.append("""
                输出要求：
                - 必须先判断用户是在提问检索问题，还是只是在闲聊、打招呼、客套对话；如果是闲聊或打招呼，只回复“暂时没有参考资料”。
                - 必须严格依据参考资料和用户问题回答。
                - 必须先判断这些参考资料是否与用户问题直接相关；如果不相关，或者没有可用参考资料，只回复“暂时没有参考资料”。
                - 对于可被参考资料支持的内容，必须保留或补充 [1]、[2] 这样的引用标记。
                - 不要编造来源，不要补充参考资料之外的事实，不要输出无引用结论。
                """);
        return builder.toString().trim();
    }

    public List<String> buildFollowUpQuestions(String question, List<RagDtos.Source> sources) {
        boolean chinese = textProcessor.containsChinese(question);
        LinkedHashSet<String> suggestions = new LinkedHashSet<>();
        for (RagDtos.Source source : sources) {
            if (chinese) {
                suggestions.add("\u5c55\u5f00\u8bb2\u8bb2\u300a" + source.title() + "\u300b\u91cc\u7684\u5173\u952e\u505a\u6cd5");
                suggestions.add("\u57fa\u4e8e\u300a" + source.title() + "\u300b\u7ed9\u51fa\u4e00\u4e2a\u843d\u5730\u6b65\u9aa4\u6e05\u5355");
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
        return List.of(
                "\u603b\u7ed3\u4e00\u4e0b\u5f53\u524d\u77e5\u8bc6\u5e93\u91cc\u548c\u8fd9\u4e2a\u95ee\u9898\u6700\u76f8\u5173\u7684\u5185\u5bb9",
                "\u628a\u76f8\u5173\u5b9e\u73b0\u62c6\u6210\u53ef\u6267\u884c\u7684\u6b65\u9aa4",
                "\u7ed9\u8fd9\u4e2a\u535a\u5ba2\u9879\u76ee\u63d0\u51fa\u4e0b\u4e00\u6b65\u53ef\u843d\u5730\u7684\u65b9\u6848"
        );
    }

    public String localizedNoDataAnswer(String question) {
        return "\u6682\u65f6\u6ca1\u6709\u53c2\u8003\u8d44\u6599";
    }

    public String localizedNoMatchAnswer(String question) {
        return "\u6682\u65f6\u6ca1\u6709\u53c2\u8003\u8d44\u6599";
    }

    private String summarizeEvidence(RagEvidence evidence) {
        return textProcessor.summarizeExcerpt(evidence.content());
    }

    private String localizedRole(String role, boolean chinese) {
        if ("assistant".equalsIgnoreCase(role)) {
            return chinese ? "\u52a9\u624b" : "Assistant";
        }
        return chinese ? "\u7528\u6237" : "User";
    }

    private String describeEvidence(RagEvidence evidence, boolean chinese) {
        if ("web".equalsIgnoreCase(evidence.sourceType())) {
            String domain = defaultValue(evidence.domain(), chinese ? "\u5916\u90e8\u7f51\u9875" : "web page");
            if (chinese) {
                return "\u6765\u6e90\uff1a\u8054\u7f51\u641c\u7d22 / " + domain + " / " + defaultValue(evidence.url(), "");
            }
            return "Source: web search / " + domain + " / " + defaultValue(evidence.url(), "");
        }

        if (chinese) {
            return "\u6765\u6e90\uff1a\u7ad9\u5185\u6587\u7ae0 / slug: " + defaultValue(evidence.slug(), evidence.reference());
        }
        return "Source: blog post / slug: " + defaultValue(evidence.slug(), evidence.reference());
    }

    private String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String stripCitationMarkers(String content) {
        return content == null ? "" : content.replaceAll("\\[(\\d+)]", "").trim();
    }
}
