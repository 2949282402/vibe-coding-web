package com.hejulian.blog.rag.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "blog.rag")
@Getter
@Setter
public class RagProperties {

    private int chunkSize = 520;
    private int chunkOverlap = 120;
    private int defaultTopK = 4;
    private double minScore = 0.2D;
    private int recallMultiplier = 4;
    private long streamTimeoutMillis = 900000L;
    private final Embedding embedding = new Embedding();
    private final Rerank rerank = new Rerank();
    private final Qdrant qdrant = new Qdrant();
    private final PythonBridge pythonBridge = new PythonBridge();
    private final Generator generator = new Generator();
    private final WebSearch webSearch = new WebSearch();

    @Getter
    @Setter
    public static class Embedding {

        private boolean enabled;
        private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
        private String apiKey;
        private String model = "text-embedding-v4";
        private int dimensions = 1024;
        private int batchSize = 10;
    }

    @Getter
    @Setter
    public static class Rerank {

        private boolean enabled;
        private String baseUrl = "https://dashscope.aliyuncs.com/compatible-api/v1";
        private String apiKey;
        private String model = "qwen3-rerank";
        private String instruct = "Given a web search query, retrieve relevant passages that answer the query.";
    }

    @Getter
    @Setter
    public static class Qdrant {

        private boolean enabled;
        private String baseUrl = "http://localhost:6333";
        private String apiKey;
        private String collection = "blog_knowledge_chunks";
        private int timeoutSeconds = 10;
        private boolean onDiskPayload = true;
    }

    @Getter
    @Setter
    public static class PythonBridge {

        private boolean enabled;
        private String baseUrl = "http://host.docker.internal:8090";
        private int timeoutSeconds = 600;
    }

    @Getter
    @Setter
    public static class Generator {

        private boolean enabled;
        private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
        private String apiKey;
        private String model = "qwen-max";
        private int timeoutSeconds = 180;
    }

    @Getter
    @Setter
    public static class WebSearch {

        private boolean enabled = true;
        private String baseUrl = "https://dashscope.aliyuncs.com";
        private String apiKey;
        private String model = "qwen-plus";
        private int timeoutSeconds = 120;
        private boolean enableSource = true;
        private boolean enableCitation;
        private String citationFormat = "[ref_<number>]";
        private boolean forcedSearch = true;
        private String searchStrategy = "turbo";
    }

}
