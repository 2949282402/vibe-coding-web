package com.hejulian.blog.rag.domain.port;

import jakarta.annotation.Nullable;
import java.util.function.Consumer;

public interface ChatModel {

    boolean isChatConfigured();

    String chatModelName();

    @Nullable
    String generate(String systemPrompt, String userPrompt, double temperature);

    @Nullable
    String streamGenerate(String systemPrompt, String userPrompt, double temperature, @Nullable Consumer<String> deltaConsumer);
}
