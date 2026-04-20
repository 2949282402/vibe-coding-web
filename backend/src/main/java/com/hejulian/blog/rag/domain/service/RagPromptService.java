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
        if (textProcessor.containsChinese(question)) {
            return """
                    \u4f60\u662f\u4e2a\u4eba\u5de5\u7a0b\u535a\u5ba2\u7684\u68c0\u7d22\u589e\u5f3a\u52a9\u624b\u3002
                    \u4f60\u53ea\u80fd\u4f9d\u636e\u7ed9\u5b9a\u8bc1\u636e\u56de\u7b54\u95ee\u9898\uff0c\u4e0d\u8981\u8865\u5145\u672a\u88ab\u8bc1\u636e\u652f\u6301\u7684\u4e8b\u5b9e\u3002
                    \u6bcf\u4e00\u6bb5\u4e8b\u5b9e\u6027\u9648\u8ff0\u90fd\u5fc5\u987b\u5e26\u4e0a\u5f15\u7528\u6807\u8bb0\uff0c\u4f8b\u5982 [1]\u3001[2]\uff0c\u4e14\u53ea\u80fd\u5f15\u7528\u5df2\u63d0\u4f9b\u7684\u7f16\u53f7\u3002
                    \u5982\u679c\u8bc1\u636e\u4e0d\u8db3\uff0c\u8bf7\u660e\u786e\u8bf4\u660e\u201c\u73b0\u6709\u8bc1\u636e\u4e0d\u8db3\u201d\uff0c\u5e76\u7ee7\u7eed\u4fdd\u6301\u6240\u6709\u5df2\u9648\u8ff0\u4e8b\u5b9e\u90fd\u6709\u5f15\u7528\u3002
                    \u4e0d\u8981\u7f16\u9020\u6765\u6e90\uff0c\u4e0d\u8981\u8f93\u51fa\u6ca1\u6709\u5f15\u7528\u652f\u6491\u7684\u7ed3\u8bba\u3002
                    """;
        }
        return """
                You are a retrieval-augmented assistant for a personal engineering blog.
                Answer only from the supplied evidence.
                Every factual paragraph must contain citation markers in the form [1], [2], and you may only cite the provided context ids.
                Do not produce uncited factual claims or unsupported citation numbers.
                If the evidence is insufficient, say so explicitly and keep all supported statements cited.
                """;
    }

    public String buildWebSearchSystemPrompt(String question) {
        if (textProcessor.containsChinese(question)) {
            return """
                    \u4f60\u662f\u4e2a\u4eba\u5de5\u7a0b\u535a\u5ba2\u7684\u68c0\u7d22\u589e\u5f3a\u52a9\u624b\u3002
                    \u8bf7\u4f18\u5148\u53c2\u8003\u6211\u63d0\u4f9b\u7684\u7ad9\u5185\u77e5\u8bc6\u7247\u6bb5\uff0c\u518d\u7ed3\u5408\u5b98\u65b9\u8054\u7f51\u641c\u7d22\u8865\u5145\u6700\u65b0\u7684\u516c\u5f00\u4fe1\u606f\u3002
                    \u5982\u679c\u7ad9\u5185\u5185\u5bb9\u4e0e\u8054\u7f51\u4fe1\u606f\u5b58\u5728\u65f6\u95f4\u5dee\u5f02\uff0c\u8bf7\u660e\u786e\u6307\u51fa\u65f6\u95f4\u8303\u56f4\uff0c\u4e0d\u8981\u6df7\u6dc6\u3002
                    \u5bf9\u7ad9\u5185\u8bc1\u636e\u652f\u6301\u7684\u5185\u5bb9\uff0c\u5c3d\u91cf\u4fdd\u7559 [1]\u3001[2] \u8fd9\u6837\u7684\u5f15\u7528\u6807\u8bb0\u3002
                    \u8054\u7f51\u8865\u5145\u90e8\u5206\u4e0d\u8981\u7f16\u9020\u6765\u6e90\uff0c\u4e0d\u786e\u5b9a\u65f6\u8bf7\u76f4\u63a5\u8bf4\u660e\u3002
                    """;
        }
        return """
                You are a retrieval-augmented assistant for a personal engineering blog.
                Prioritize the provided local blog evidence, then use official web search to supplement newer public information.
                If local evidence and web information differ because of timing, make the timeline explicit instead of blending them.
                Preserve local citations like [1] and [2] whenever the statement is supported by local evidence.
                Do not invent web facts or sources.
                """;
    }

    public String buildUserPrompt(String question, List<RagEvidence> evidences, List<ChatHistoryMessage> historyWindow) {
        boolean chinese = textProcessor.containsChinese(question);
        StringBuilder builder = new StringBuilder();

        builder.append(chinese ? "\u95ee\u9898\uff1a\n" : "Question:\n")
                .append(question.trim())
                .append("\n\n");

        if (!historyWindow.isEmpty()) {
            builder.append(chinese ? "\u6700\u8fd1\u5bf9\u8bdd\uff1a\n" : "Conversation History:\n");
            for (ChatHistoryMessage message : historyWindow) {
                builder.append("- ")
                        .append(localizedRole(message.role(), chinese))
                        .append(": ")
                        .append(stripCitationMarkers(message.content()))
                        .append("\n");
            }
            builder.append("\n");
        }

        builder.append(chinese ? "\u8bc1\u636e\u4e0a\u4e0b\u6587\uff1a\n" : "Context:\n");
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

        builder.append(chinese ? "\u8f93\u51fa\u8981\u6c42\uff1a\n" : "Output requirement:\n");
        if (chinese) {
            builder.append("- \u53ea\u80fd\u4f7f\u7528\u4e0a\u9762\u7684\u8bc1\u636e\u56de\u7b54\u3002\n")
                    .append("- \u6bcf\u4e00\u6bb5\u4e8b\u5b9e\u6027\u5185\u5bb9\u90fd\u5fc5\u987b\u5305\u542b [1] \u6216 [1][2] \u8fd9\u6837\u7684\u5f15\u7528\u3002\n")
                    .append("- \u4e0d\u8981\u5f15\u7528\u672a\u51fa\u73b0\u8fc7\u7684\u7f16\u53f7\u3002\n")
                    .append("- \u5982\u679c\u7ad9\u5185\u8bc1\u636e\u548c\u8054\u7f51\u7ed3\u679c\u90fd\u4e0d\u8db3\uff0c\u8bf7\u76f4\u63a5\u8bf4\u660e\u8bc1\u636e\u4e0d\u8db3\u3002\n");
        } else {
            builder.append("- Use only the evidence above.\n")
                    .append("- Every factual paragraph must include citations like [1] or [1][2].\n")
                    .append("- Do not mention any citation id that is not listed above.\n")
                    .append("- If the local and web evidence are both insufficient, say so explicitly.\n");
        }
        return builder.toString();
    }

    public String buildWebSearchUserPrompt(String question, List<RagEvidence> localEvidences, List<ChatHistoryMessage> historyWindow) {
        boolean chinese = textProcessor.containsChinese(question);
        StringBuilder builder = new StringBuilder();

        builder.append(chinese ? "\u7528\u6237\u95ee\u9898\uff1a\n" : "Question:\n")
                .append(question.trim())
                .append("\n\n");

        if (!historyWindow.isEmpty()) {
            builder.append(chinese ? "\u6700\u8fd1\u5bf9\u8bdd\uff1a\n" : "Recent conversation:\n");
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
            builder.append(chinese
                    ? "\u5f53\u524d\u6ca1\u6709\u8db3\u591f\u7684\u7ad9\u5185\u77e5\u8bc6\u7247\u6bb5\uff0c\u4f60\u9700\u8981\u91cd\u70b9\u4f7f\u7528\u8054\u7f51\u641c\u7d22\u8865\u5145\u7b54\u6848\u3002\n\n"
                    : "There is no strong local blog evidence yet, so rely primarily on web search.\n\n");
        } else {
            builder.append(chinese ? "\u7ad9\u5185\u77e5\u8bc6\u7247\u6bb5\uff1a\n" : "Local blog evidence:\n");
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

        if (chinese) {
            builder.append("""
                    ?????
                    - ??????????????????
                    - ???????????????????????? [1]?[2] ???
                    - ????????????????????????
                    - ????????????????
                    """);
        } else {
            builder.append("""
                    Output requirement:
                    - Answer the core question first, then add only the needed context.
                    - Keep local citations like [1] and [2] for claims directly supported by local evidence.
                    - Use web search only to supplement newer public information; do not fabricate sources.
                    - If web search is still insufficient, say so clearly.
                    """);
        }
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
        if (textProcessor.containsChinese(question)) {
            return List.of(
                    "\u603b\u7ed3\u4e00\u4e0b\u5f53\u524d\u77e5\u8bc6\u5e93\u91cc\u548c\u8fd9\u4e2a\u95ee\u9898\u6700\u76f8\u5173\u7684\u5185\u5bb9",
                    "\u628a\u76f8\u5173\u5b9e\u73b0\u62c6\u6210\u53ef\u6267\u884c\u7684\u6b65\u9aa4",
                    "\u7ed9\u8fd9\u4e2a\u535a\u5ba2\u9879\u76ee\u63d0\u51fa\u4e0b\u4e00\u6b65\u53ef\u843d\u5730\u7684\u65b9\u6848"
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
            return "\u5f53\u524d\u77e5\u8bc6\u5e93\u91cc\u8fd8\u6ca1\u6709\u53ef\u7528\u4e8e\u5f15\u7528\u56de\u7b54\u7684\u5df2\u53d1\u5e03\u6587\u7ae0\uff0c\u56e0\u6b64\u6682\u65f6\u65e0\u6cd5\u751f\u6210\u5e26\u5f15\u7528\u7684 RAG \u56de\u7b54\u3002";
        }
        return "The knowledge base does not contain any published posts yet, so no cited RAG answer can be generated.";
    }

    public String localizedNoMatchAnswer(String question) {
        if (textProcessor.containsChinese(question)) {
            return "\u6211\u6682\u65f6\u6ca1\u6709\u5728\u5f53\u524d\u77e5\u8bc6\u5e93\u6216\u8054\u7f51\u7ed3\u679c\u91cc\u627e\u5230\u8db3\u591f\u76f8\u5173\u7684\u8bc1\u636e\u3002\u4f60\u53ef\u4ee5\u6362\u4e00\u4e2a\u66f4\u5177\u4f53\u7684\u95ee\u9898\uff0c\u6216\u8005\u8865\u5145\u66f4\u591a\u76f8\u5173\u5185\u5bb9\u3002";
        }
        return "I could not find enough relevant evidence in the current knowledge base or web results. Try a more specific query or add more related content.";
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
