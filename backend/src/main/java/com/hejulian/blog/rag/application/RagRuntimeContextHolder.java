package com.hejulian.blog.rag.application;

import jakarta.annotation.Nullable;

public final class RagRuntimeContextHolder {

    private static final ThreadLocal<RagRuntimeOptions> CONTEXT = new ThreadLocal<>();

    private RagRuntimeContextHolder() {
    }

    public static void set(@Nullable RagRuntimeOptions options) {
        if (options == null) {
            CONTEXT.remove();
            return;
        }
        CONTEXT.set(options);
    }

    @Nullable
    public static RagRuntimeOptions get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }

    public record RagRuntimeOptions(
            String apiKey,
            String chatModel,
            boolean webSearchEnabled
    ) {
    }
}
