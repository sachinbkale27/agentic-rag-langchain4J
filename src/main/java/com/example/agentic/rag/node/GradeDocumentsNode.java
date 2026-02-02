package com.example.agentic.rag.node;

import com.example.agentic.rag.chain.RetrievalGrader;
import com.example.agentic.rag.model.GradeDocuments;
import com.example.agentic.rag.model.GraphState;
import com.example.agentic.rag.service.LangSmithTracingService;
import dev.langchain4j.data.document.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Determines whether the retrieved documents are relevant to the question
 */
@Component
public class GradeDocumentsNode {
    private static final Logger log = LoggerFactory.getLogger(GradeDocumentsNode.class);

    private final RetrievalGrader retrievalGrader;
    private final LangSmithTracingService tracingService;

    public GradeDocumentsNode(RetrievalGrader retrievalGrader, LangSmithTracingService tracingService) {
        this.retrievalGrader = retrievalGrader;
        this.tracingService = tracingService;
    }

    public GraphState gradeDocuments(GraphState state) {
        log.info("---CHECK DOCUMENT RELEVANCE TO QUESTION---");
        String question = state.getQuestion();
        List<Document> documents = state.getDocuments();

        // Trace document grading
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("question", question);
        inputs.put("num_documents", documents.size());
        String runId = tracingService.startRun("chain", "GradeDocuments", inputs);

        List<Document> filteredDocs = new ArrayList<>();
        boolean webSearch = false;

        for (Document doc : documents) {
            GradeDocuments score = retrievalGrader.grade(doc.text(), question);
            String grade = score.getBinaryScore();

            if ("yes".equalsIgnoreCase(grade)) {
                log.info("--GRADE: DOCUMENT RELEVANT--");
                filteredDocs.add(doc);
            } else {
                log.info("--GRADE: DOCUMENT NOT RELEVANT--");
                webSearch = true;
            }
        }

        // End trace
        Map<String, Object> outputs = new HashMap<>();
        outputs.put("relevant_documents", filteredDocs.size());
        outputs.put("total_documents", documents.size());
        outputs.put("needs_web_search", webSearch);
        tracingService.endRun(runId, outputs, null);

        return GraphState.builder()
                .question(question)
                .documents(filteredDocs)
                .webSearch(webSearch)
                .build();
    }
}
