package com.hejulian.blog.rag.domain.port;

import jakarta.annotation.Nullable;
import java.util.function.Consumer;
import com.hejulian.blog.rag.domain.model.WebSearchAnswer;

public interface ChatModel {

    boolean isChatConfigured();

    String chatModelName();

    @Nullable
    String generate(String systemPrompt, String userPrompt, double temperature);

    @Nullable
    String streamGenerate(String systemPrompt, String userPrompt, double temperature, @Nullable Consumer<String> deltaConsumer);

    default boolean supportsWebSearch() {
        return false;
    }

    @Nullable
    default WebSearchAnswer generateWithWebSearch(String systemPrompt, String userPrompt, double temperature) {
        return null;
    }
}
