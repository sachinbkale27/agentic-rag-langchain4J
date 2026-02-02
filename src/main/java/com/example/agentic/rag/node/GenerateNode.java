package com.example.agentic.rag.node;

import com.example.agentic.rag.chain.GenerationChain;
import com.example.agentic.rag.model.GraphState;
import com.example.agentic.rag.service.LangSmithTracingService;
import dev.langchain4j.data.document.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Generates an answer based on the question and documents
 */
@Component
public class GenerateNode {
    private static final Logger log = LoggerFactory.getLogger(GenerateNode.class);

    private final GenerationChain generationChain;
    private final LangSmithTracingService tracingService;

    public GenerateNode(GenerationChain generationChain, LangSmithTracingService tracingService) {
        this.generationChain = generationChain;
        this.tracingService = tracingService;
    }

    public GraphState generate(GraphState state) {
        log.info("---GENERATE---");
        String question = state.getQuestion();
        var documents = state.getDocuments();

        // Convert documents to context string
        String context = documents.stream()
                .map(Document::text)
                .collect(Collectors.joining("\n\n"));

        // Trace generation
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("question", question);
        inputs.put("context", context.substring(0, Math.min(500, context.length())) + "...");
        inputs.put("num_documents", documents.size());
        String runId = tracingService.startRun("llm", "GenerateAnswer", inputs);

        String generation = generationChain.generate(context, question);

        // End trace
        Map<String, Object> outputs = new HashMap<>();
        outputs.put("generation", generation);
        tracingService.endRun(runId, outputs, null);

        return GraphState.builder()
                .question(question)
                .documents(documents)
                .generation(generation)
                .build();
    }
}
