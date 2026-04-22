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
                    \u4f60\u53ea\u80fd\u4f9d\u636e\u7ed9\u5b9a\u7684\u53c2\u8003\u8d44\u6599\u548c\u7528\u6237\u95ee\u9898\u56de\u7b54\uff0c\u4e0d\u8981\u8865\u5145\u4efb\u4f55\u672a\u88ab\u53c2\u8003\u8d44\u6599\u76f4\u63a5\u652f\u6301\u7684\u4e8b\u5b9e\u3001\u63a8\u65ad\u3001\u5e38\u8bc6\u6216\u5ef6\u4f38\u7ed3\u8bba\u3002
                    \u5982\u679c\u53c2\u8003\u8d44\u6599\u4e3a\u7a7a\uff0c\u6216\u8005\u53c2\u8003\u8d44\u6599\u4e0e\u7528\u6237\u95ee\u9898\u4e0d\u76f8\u5173\uff0c\u5fc5\u987b\u53ea\u56de\u590d\u201c\u6682\u65f6\u6ca1\u6709\u53c2\u8003\u8d44\u6599\u201d\u3002
                    \u6bcf\u4e00\u6bb5\u4e8b\u5b9e\u6027\u9648\u8ff0\u90fd\u5fc5\u987b\u5e26\u4e0a\u5f15\u7528\u6807\u8bb0\uff0c\u4f8b\u5982 [1]\u3001[2]\uff0c\u4e14\u53ea\u80fd\u5f15\u7528\u5df2\u63d0\u4f9b\u7684\u7f16\u53f7\u3002
                    \u4e0d\u8981\u7f16\u9020\u6765\u6e90\uff0c\u4e0d\u8981\u8f93\u51fa\u6ca1\u6709\u5f15\u7528\u652f\u6491\u7684\u7ed3\u8bba\uff0c\u4e0d\u8981\u6839\u636e\u8bad\u7ec3\u77e5\u8bc6\u81ea\u884c\u8865\u5168\u7b54\u6848\u3002
                    """;
        }
        return """
                You are a retrieval-augmented assistant for a personal engineering blog.
                Answer only from the supplied references and the user's question.
                If the references are empty or unrelated to the user's question, reply only with "No reference materials are available at the moment."
                Every factual paragraph must contain citation markers in the form [1], [2], and you may only cite the provided context ids.
                Do not produce uncited factual claims, unsupported citation numbers, or knowledge from outside the provided references.
                """;
    }

    public String buildWebSearchSystemPrompt(String question) {
        if (textProcessor.containsChinese(question)) {
            return """
                    \u4f60\u662f\u4e2a\u4eba\u5de5\u7a0b\u535a\u5ba2\u7684\u68c0\u7d22\u589e\u5f3a\u52a9\u624b\u3002
                    \u8bf7\u4e25\u683c\u4f9d\u636e\u6211\u63d0\u4f9b\u7684\u53c2\u8003\u8d44\u6599\u548c\u7528\u6237\u95ee\u9898\u4f5c\u7b54\u3002
                    \u53ef\u4ee5\u53c2\u8003\u7ad9\u5185\u77e5\u8bc6\u7247\u6bb5\uff0c\u4e5f\u53ef\u4ee5\u53c2\u8003\u8054\u7f51\u641c\u7d22\u7ed3\u679c\uff0c\u4f46\u90fd\u5fc5\u987b\u4e0e\u7528\u6237\u95ee\u9898\u76f4\u63a5\u76f8\u5173\u3002
                    \u5982\u679c\u53c2\u8003\u8d44\u6599\u4e3a\u7a7a\uff0c\u6216\u8005\u53c2\u8003\u8d44\u6599\u4e0e\u7528\u6237\u95ee\u9898\u4e0d\u76f8\u5173\uff0c\u5fc5\u987b\u53ea\u56de\u590d\u201c\u6682\u65f6\u6ca1\u6709\u53c2\u8003\u8d44\u6599\u201d\u3002
                    \u5bf9\u53c2\u8003\u8d44\u6599\u652f\u6301\u7684\u5185\u5bb9\uff0c\u5fc5\u987b\u4fdd\u7559\u6216\u8865\u5145 [1]\u3001[2] \u8fd9\u6837\u7684\u5f15\u7528\u6807\u8bb0\u3002
                    \u4e0d\u8981\u7f16\u9020\u6765\u6e90\uff0c\u4e0d\u8981\u8f93\u51fa\u6ca1\u6709\u5f15\u7528\u652f\u6491\u7684\u7ed3\u8bba\uff0c\u4e0d\u8981\u6839\u636e\u8bad\u7ec3\u77e5\u8bc6\u81ea\u884c\u8865\u5168\u7b54\u6848\u3002
                    """;
        }
        return """
                You are a retrieval-augmented assistant for a personal engineering blog.
                Answer strictly from the provided references and the user's question.
                You may use local evidence and web search results, but only when they are directly relevant to the user's question.
                If the references are empty or unrelated to the user's question, reply only with "No reference materials are available at the moment."
                Preserve citations like [1] and [2] whenever a statement is supported by the provided references.
                Do not invent facts, sources, or unsupported conclusions.
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
                    .append("- \u5fc5\u987b\u5148\u5224\u65ad\u53c2\u8003\u8d44\u6599\u662f\u5426\u80fd\u76f4\u63a5\u56de\u7b54\u7528\u6237\u95ee\u9898\uff1b\u5982\u679c\u4e0d\u80fd\u76f4\u63a5\u56de\u7b54\uff0c\u6216\u53c2\u8003\u8d44\u6599\u4e0d\u76f8\u5173\uff0c\u53ea\u56de\u590d\u201c\u6682\u65f6\u6ca1\u6709\u53c2\u8003\u8d44\u6599\u201d\u3002\n")
                    .append("- \u6bcf\u4e00\u6bb5\u4e8b\u5b9e\u6027\u5185\u5bb9\u90fd\u5fc5\u987b\u5305\u542b [1] \u6216 [1][2] \u8fd9\u6837\u7684\u5f15\u7528\u3002\n")
                    .append("- \u4e0d\u8981\u5f15\u7528\u672a\u51fa\u73b0\u8fc7\u7684\u7f16\u53f7\u3002\n")
                    .append("- \u4e0d\u8981\u4f7f\u7528\u53c2\u8003\u8d44\u6599\u4e4b\u5916\u7684\u5e38\u8bc6\u3001\u7ecf\u9a8c\u6216\u8bad\u7ec3\u77e5\u8bc6\u8865\u5145\u7b54\u6848\u3002\n");
        } else {
            builder.append("- Use only the evidence above.\n")
                    .append("- First judge whether the references directly answer the user's question; if not, reply only with \"No reference materials are available at the moment.\"\n")
                    .append("- Every factual paragraph must include citations like [1] or [1][2].\n")
                    .append("- Do not mention any citation id that is not listed above.\n")
                    .append("- Do not supplement the answer with knowledge outside the provided references.\n");
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
                    \u8f93\u51fa\u8981\u6c42\uff1a
                    - \u5fc5\u987b\u4e25\u683c\u4f9d\u636e\u53c2\u8003\u8d44\u6599\u548c\u7528\u6237\u95ee\u9898\u56de\u7b54\u3002
                    - \u5fc5\u987b\u5148\u5224\u65ad\u8fd9\u4e9b\u53c2\u8003\u8d44\u6599\u662f\u5426\u4e0e\u7528\u6237\u95ee\u9898\u76f4\u63a5\u76f8\u5173\uff1b\u5982\u679c\u4e0d\u76f8\u5173\uff0c\u6216\u8005\u6ca1\u6709\u53ef\u7528\u53c2\u8003\u8d44\u6599\uff0c\u53ea\u56de\u590d\u201c\u6682\u65f6\u6ca1\u6709\u53c2\u8003\u8d44\u6599\u201d\u3002
                    - \u5bf9\u4e8e\u53ef\u88ab\u53c2\u8003\u8d44\u6599\u652f\u6301\u7684\u5185\u5bb9\uff0c\u5fc5\u987b\u4fdd\u7559\u6216\u8865\u5145 [1]\u3001[2] \u8fd9\u6837\u7684\u5f15\u7528\u6807\u8bb0\u3002
                    - \u4e0d\u8981\u7f16\u9020\u6765\u6e90\uff0c\u4e0d\u8981\u8865\u5145\u53c2\u8003\u8d44\u6599\u4e4b\u5916\u7684\u4e8b\u5b9e\uff0c\u4e0d\u8981\u8f93\u51fa\u65e0\u5f15\u7528\u7ed3\u8bba\u3002
                    """);
        } else {
            builder.append("""
                    Output requirement:
                    - Answer strictly from the provided references and the user's question.
                    - First judge whether the references are directly relevant; if not, reply only with "No reference materials are available at the moment."
                    - Keep citations like [1] and [2] for claims directly supported by the provided references.
                    - Do not fabricate sources or add facts beyond the provided references.
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
            return "\u6682\u65f6\u6ca1\u6709\u53c2\u8003\u8d44\u6599";
        }
        return "No reference materials are available at the moment.";
    }

    public String localizedNoMatchAnswer(String question) {
        if (textProcessor.containsChinese(question)) {
            return "\u6682\u65f6\u6ca1\u6709\u53c2\u8003\u8d44\u6599";
        }
        return "No reference materials are available at the moment.";
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
