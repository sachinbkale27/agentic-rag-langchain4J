package com.example.agentic.rag.node;

import com.example.agentic.rag.model.GraphState;
import com.example.agentic.rag.service.LangSmithTracingService;
import com.example.agentic.rag.service.VectorStoreService;
import dev.langchain4j.data.document.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Retrieves documents from the vector store
 */
@Component
public class RetrieveNode {
    private static final Logger log = LoggerFactory.getLogger(RetrieveNode.class);

    private final VectorStoreService vectorStoreService;
    private final LangSmithTracingService tracingService;

    public RetrieveNode(VectorStoreService vectorStoreService, LangSmithTracingService tracingService) {
        this.vectorStoreService = vectorStoreService;
        this.tracingService = tracingService;
    }

    public GraphState retrieve(GraphState state) {
        log.info("---RETRIEVE---");
        String question = state.getQuestion();

        // Trace retrieval
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("question", question);
        String runId = tracingService.startRun("retriever", "RetrieveDocuments", inputs);

        List<Document> documents = vectorStoreService.retrieveDocuments(question);

        // End trace
        Map<String, Object> outputs = new HashMap<>();
        outputs.put("num_documents", documents.size());
        outputs.put("documents", documents.stream()
                .map(doc -> doc.text().substring(0, Math.min(200, doc.text().length())) + "...")
                .toList());
        tracingService.endRun(runId, outputs, null);

        return GraphState.builder()
                .question(question)
                .documents(documents)
                .build();
    }
}
