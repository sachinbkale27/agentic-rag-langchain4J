package com.sachin.agentic.rag.node;

import com.sachin.agentic.rag.model.GraphState;
import com.sachin.agentic.rag.service.LangSmithTracingService;
import com.sachin.agentic.rag.service.TavilySearchService;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Performs web search using Tavily
 */
@Component
public class WebSearchNode {
    private static final Logger log = LoggerFactory.getLogger(WebSearchNode.class);

    private final TavilySearchService tavilySearchService;
    private final LangSmithTracingService tracingService;

    public WebSearchNode(TavilySearchService tavilySearchService, LangSmithTracingService tracingService) {
        this.tavilySearchService = tavilySearchService;
        this.tracingService = tracingService;
    }

    public GraphState webSearch(GraphState state) {
        log.info("--WEB_SEARCH--");
        String question = state.getQuestion();
        List<Document> existingDocs = state.getDocuments() != null ? state.getDocuments() : new ArrayList<>();

        // Trace web search
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("question", question);
        String runId = tracingService.startRun("tool", "WebSearch", inputs);

        // Perform web search
        String searchResults = tavilySearchService.search(question);
        Document webSearchDoc = Document.from(searchResults, Metadata.from("source", "web_search"));

        List<Document> documents = new ArrayList<>(existingDocs);
        documents.add(webSearchDoc);

        // End trace
        Map<String, Object> outputs = new HashMap<>();
        outputs.put("search_results_length", searchResults.length());
        outputs.put("total_documents", documents.size());
        tracingService.endRun(runId, outputs, null);

        return GraphState.builder()
                .question(question)
                .documents(documents)
                .build();
    }
}
