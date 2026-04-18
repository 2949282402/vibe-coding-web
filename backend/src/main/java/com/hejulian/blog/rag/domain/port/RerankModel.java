package com.hejulian.blog.rag.domain.port;

import com.hejulian.blog.rag.domain.model.RerankResult;
import java.util.List;

public interface RerankModel {

    boolean isRerankConfigured();

    List<RerankResult> rerank(String query, List<String> documents, int topN);
}
