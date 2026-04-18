package com.hejulian.blog.rag.domain.service;

import com.hejulian.blog.rag.domain.model.KnowledgeChunk;
import com.hejulian.blog.rag.domain.model.PublishedPost;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class KnowledgeTextProcessor {

    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("(?s)```.*?```");
    private static final Pattern INLINE_CODE_PATTERN = Pattern.compile("`([^`]*)`");
    private static final Pattern IMAGE_PATTERN = Pattern.compile("!\\[[^\\]]*]\\(([^)]+)\\)");
    private static final Pattern LINK_PATTERN = Pattern.compile("\\[([^\\]]+)]\\(([^)]+)\\)");
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern MULTI_SPACE_PATTERN = Pattern.compile("\\s+");
    private static final Pattern CHINESE_PATTERN = Pattern.compile("[\\u4e00-\\u9fff]+");

    public List<KnowledgeChunk> buildChunks(List<PublishedPost> posts, int chunkSize, int chunkOverlap) {
        List<KnowledgeChunk> chunks = new ArrayList<>();
        for (PublishedPost post : posts) {
            String plainText = toPlainText(post);
            if (!StringUtils.hasText(plainText)) {
                continue;
            }

            List<String> segments = splitIntoChunks(plainText, chunkSize, chunkOverlap);
            for (int index = 0; index < segments.size(); index++) {
                chunks.add(new KnowledgeChunk(
                        post.id(),
                        post.title(),
                        post.slug(),
                        index,
                        segments.get(index),
                        post.publishedAt(),
                        null,
                        null
                ));
            }
        }
        return chunks;
    }

    public String buildFingerprint(List<PublishedPost> posts) {
        LocalDateTime latest = posts.stream()
                .map(post -> post.updatedAt() != null ? post.updatedAt() : post.publishedAt())
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(LocalDateTime.MIN);
        return posts.size() + "|" + latest.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    public String normalizeWhitespace(String text) {
        return MULTI_SPACE_PATTERN.matcher(safe(text).replace('\u00A0', ' ')).replaceAll(" ").trim();
    }

    public String summarizeExcerpt(String text) {
        String normalized = normalizeWhitespace(text);
        if (normalized.length() <= 180) {
            return normalized;
        }
        return normalized.substring(0, 180).trim() + "...";
    }

    public boolean containsChinese(String text) {
        return CHINESE_PATTERN.matcher(safe(text)).find();
    }

    public String safeLower(String value) {
        return safe(value).toLowerCase(Locale.ROOT);
    }

    private String toPlainText(PublishedPost post) {
        String merged = String.join(
                "\n\n",
                safe(post.title()),
                safe(post.summary()),
                markdownToPlainText(post.content())
        );
        return normalizeWhitespace(merged);
    }

    private List<String> splitIntoChunks(String plainText, int chunkSize, int chunkOverlap) {
        List<String> chunks = new ArrayList<>();
        String text = normalizeWhitespace(plainText);
        if (!StringUtils.hasText(text)) {
            return chunks;
        }

        int normalizedChunkSize = Math.max(chunkSize, 240);
        int normalizedOverlap = Math.min(Math.max(chunkOverlap, 40), normalizedChunkSize / 2);
        int start = 0;

        while (start < text.length()) {
            int tentativeEnd = Math.min(start + normalizedChunkSize, text.length());
            int end = findChunkBoundary(text, start, tentativeEnd);
            String chunk = text.substring(start, end).trim();
            if (StringUtils.hasText(chunk)) {
                chunks.add(chunk);
            }
            if (end >= text.length()) {
                break;
            }
            start = Math.max(end - normalizedOverlap, start + 1);
        }

        return chunks;
    }

    private int findChunkBoundary(String text, int start, int tentativeEnd) {
        if (tentativeEnd >= text.length()) {
            return text.length();
        }
        for (int cursor = tentativeEnd; cursor > start + 120; cursor--) {
            char current = text.charAt(cursor - 1);
            if (current == '\n' || current == '。' || current == '！' || current == '？'
                    || current == '.' || current == '!' || current == '?') {
                return cursor;
            }
        }
        return tentativeEnd;
    }

    private String markdownToPlainText(String markdown) {
        String text = safe(markdown);
        text = CODE_BLOCK_PATTERN.matcher(text).replaceAll(" ");
        text = INLINE_CODE_PATTERN.matcher(text).replaceAll("$1");
        text = IMAGE_PATTERN.matcher(text).replaceAll(" ");
        text = LINK_PATTERN.matcher(text).replaceAll("$1");
        text = text.replaceAll("(?m)^#{1,6}\\s*", " ");
        text = text.replaceAll("(?m)^>+\\s*", " ");
        text = text.replaceAll("(?m)^[-*+]\\s+", " ");
        text = text.replaceAll("(?m)^\\d+\\.\\s+", " ");
        text = text.replaceAll("[*_~|]+", " ");
        text = HTML_TAG_PATTERN.matcher(text).replaceAll(" ");
        return normalizeWhitespace(text);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
