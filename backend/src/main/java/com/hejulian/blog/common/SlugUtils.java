package com.hejulian.blog.common;

import java.text.Normalizer;
import java.util.Locale;

public final class SlugUtils {

    private SlugUtils() {
    }

    public static String toSlug(String source) {
        if (source == null || source.isBlank()) {
            return "item";
        }

        String original = source.trim();
        String normalized = Normalizer.normalize(source, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");

        if (!normalized.isBlank()) {
            return normalized;
        }

        return "item-" + Integer.toHexString(original.hashCode());
    }
}
