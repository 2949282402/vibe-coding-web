package com.hejulian.blog.rag.domain.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CitationGuardService {

    private static final Pattern CITATION_PATTERN = Pattern.compile("\\[(\\d+)]");
    private static final Pattern CONTENT_PATTERN = Pattern.compile("[\\p{L}\\p{N}\\u4e00-\\u9fff]");

    public boolean isStrictlyCited(String answer, int sourceCount) {
        if (!StringUtils.hasText(answer) || sourceCount <= 0) {
            return false;
        }

        List<String> segments = splitSegments(answer);
        if (segments.isEmpty()) {
            return false;
        }

        boolean hasCitation = false;
        for (String segment : segments) {
            if (!CONTENT_PATTERN.matcher(segment).find()) {
                continue;
            }
            Matcher matcher = CITATION_PATTERN.matcher(segment);
            boolean segmentHasCitation = false;
            while (matcher.find()) {
                segmentHasCitation = true;
                hasCitation = true;
                int index = Integer.parseInt(matcher.group(1));
                if (index < 1 || index > sourceCount) {
                    return false;
                }
            }
            if (!segmentHasCitation) {
                return false;
            }
        }
        return hasCitation;
    }

    public List<Integer> extractCitationIndices(String answer, int sourceCount) {
        if (!StringUtils.hasText(answer) || sourceCount <= 0) {
            return List.of();
        }

        Set<Integer> citations = new LinkedHashSet<>();
        Matcher matcher = CITATION_PATTERN.matcher(answer);
        while (matcher.find()) {
            int index = Integer.parseInt(matcher.group(1));
            if (index >= 1 && index <= sourceCount) {
                citations.add(index);
            }
        }
        return List.copyOf(citations);
    }

    private List<String> splitSegments(String answer) {
        String normalized = answer.replace("\r", "\n");
        String[] rawSegments = normalized.split("\\n\\s*\\n|(?<=[。！？.!?])\\s+");
        List<String> segments = new ArrayList<>();
        for (String rawSegment : rawSegments) {
            String segment = rawSegment.trim();
            if (StringUtils.hasText(segment)) {
                segments.add(segment);
            }
        }
        return segments;
    }
}
