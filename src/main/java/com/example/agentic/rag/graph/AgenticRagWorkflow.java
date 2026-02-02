package com.example.agentic.rag.graph;

import com.example.agentic.rag.chain.AnswerGrader;
import com.example.agentic.rag.chain.HallucinationGrader;
import com.example.agentic.rag.chain.QuestionRouter;
import com.example.agentic.rag.model.GradeAnswer;
import com.example.agentic.rag.model.GradeHallucinations;
import com.example.agentic.rag.model.GraphState;
import com.example.agentic.rag.model.RouteQuery;
import com.example.agentic.rag.node.GenerateNode;
import com.example.agentic.rag.node.GradeDocumentsNode;
import com.example.agentic.rag.node.RetrieveNode;
import com.example.agentic.rag.node.WebSearchNode;
import com.example.agentic.rag.service.LangSmithTracingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static com.example.agentic.rag.model.GraphConstants.*;

/**
 * Orchestrates the agentic RAG workflow
 */
@Component
public class AgenticRagWorkflow {
    private static final Logger log = LoggerFactory.getLogger(AgenticRagWorkflow.class);

    private final QuestionRouter questionRouter;
    private final RetrieveNode retrieveNode;
    private final GradeDocumentsNode gradeDocumentsNode;
    private final GenerateNode generateNode;
    private final WebSearchNode webSearchNode;
    private final HallucinationGrader hallucinationGrader;
    private final AnswerGrader answerGrader;
    private final LangSmithTracingService tracingService;

    public AgenticRagWorkflow(QuestionRouter questionRouter, RetrieveNode retrieveNode,
                              GradeDocumentsNode gradeDocumentsNode, GenerateNode generateNode,
                              WebSearchNode webSearchNode, HallucinationGrader hallucinationGrader,
                              AnswerGrader answerGrader, LangSmithTracingService tracingService) {
        this.questionRouter = questionRouter;
        this.retrieveNode = retrieveNode;
        this.gradeDocumentsNode = gradeDocumentsNode;
        this.generateNode = generateNode;
        this.webSearchNode = webSearchNode;
        this.hallucinationGrader = hallucinationGrader;
        this.answerGrader = answerGrader;
        this.tracingService = tracingService;
    }

    public GraphState invoke(String question) {
        log.info("Starting workflow for question: {}", question);

        // Start LangSmith trace for the entire workflow
        Map<String, Object> workflowInputs = new HashMap<>();
        workflowInputs.put("question", question);
        String workflowRunId = tracingService.startRun("chain", "AgenticRAGWorkflow", workflowInputs);

        GraphState state = GraphState.builder()
                .question(question)
                .build();

        // Entry point: Route question
        String route = routeQuestion(state);

        if (WEBSEARCH.equals(route)) {
            state = webSearchNode.webSearch(state);
            state = generateNode.generate(state);
        } else {
            // Retrieve from vector store
            state = retrieveNode.retrieve(state);

            // Grade documents
            state = gradeDocumentsNode.gradeDocuments(state);

            // Decide whether to generate or web search
            String decision = decideToGenerate(state);

            if (WEBSEARCH.equals(decision)) {
                state = webSearchNode.webSearch(state);
            }

            // Generate answer
            state = generateNode.generate(state);

            // Check for hallucinations and answer quality
            state = checkGenerationQuality(state);
        }

        log.info("Workflow completed. Final answer: {}", state.getGeneration());

        // End LangSmith trace
        Map<String, Object> workflowOutputs = new HashMap<>();
        workflowOutputs.put("question", state.getQuestion());
        workflowOutputs.put("answer", state.getGeneration());
        workflowOutputs.put("documents_used", state.getDocuments() != null ? state.getDocuments().size() : 0);
        tracingService.endRun(workflowRunId, workflowOutputs, null);

        return state;
    }

    private String routeQuestion(GraphState state) {
        log.info("---ROUTE QUESTION---");
        String question = state.getQuestion();

        // Trace routing decision
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("question", question);
        String runId = tracingService.startRun("chain", "RouteQuestion", inputs);

        RouteQuery source = questionRouter.route(question);

        Map<String, Object> outputs = new HashMap<>();
        outputs.put("datasource", source.getDatasource());
        tracingService.endRun(runId, outputs, null);

        if (WEBSEARCH.equals(source.getDatasource())) {
            log.info("---ROUTE QUESTION TO WEBSEARCH---");
            return WEBSEARCH;
        } else if (VECTORSTORE.equals(source.getDatasource())) {
            log.info("---ROUTE QUESTION TO RAG---");
            return RETRIEVE;
        } else {
            log.info("---NONE ROUTE QUESTION TO WEBSEARCH---");
            return WEBSEARCH;
        }
    }

    private String decideToGenerate(GraphState state) {
        log.info("---ASSESS GRADED DOCUMENTS---");
        if (state.isWebSearch()) {
            log.info("--DECISION: NOT ALL DOCUMENTS ARE RELEVANT TO THE QUESTION--");
            return WEBSEARCH;
        } else {
            log.info("--DECISION: GENERATE--");
            return GENERATE;
        }
    }

    private GraphState checkGenerationQuality(GraphState state) {
        log.info("---CHECK HALLUCINATIONS---");
        String question = state.getQuestion();
        String generation = state.getGeneration();
        String documents = state.getDocuments().stream()
                .map(dev.langchain4j.data.document.Document::text)
                .collect(Collectors.joining("\n\n"));

        GradeHallucinations hallucinationScore = hallucinationGrader.grade(documents, generation);

        if (hallucinationScore.isBinaryScore()) {
            log.info("--DECISION: GENERATION IS GROUNDED IN DOCUMENTS--");
            log.info("--GRADE GENERATION vs QUESTION--");

            GradeAnswer answerScore = answerGrader.grade(question, generation);

            if (answerScore.isBinaryScore()) {
                log.info("--DECISION: GENERATION ADDRESSES QUESTION--");
                return state; // Success
            } else {
                log.info("--DECISION: GENERATION DOES NOT ADDRESS QUESTION--");
                // Regenerate with web search
                state = webSearchNode.webSearch(state);
                state = generateNode.generate(state);
                return state;
            }
        } else {
            log.info("--DECISION: GENERATION IS NOT GROUNDED IN DOCUMENTS--");
            // Regenerate
            state = generateNode.generate(state);
            return checkGenerationQuality(state); // Recursive check
        }
    }
}
