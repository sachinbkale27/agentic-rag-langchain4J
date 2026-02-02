package com.sachin.agentic.rag;

import com.sachin.agentic.rag.graph.AgenticRagWorkflow;
import com.sachin.agentic.rag.model.GraphState;
import com.sachin.agentic.rag.service.IngestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.List;

@SpringBootApplication
public class AgenticRagApplication implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(AgenticRagApplication.class);

    private final AgenticRagWorkflow workflow;
    private final IngestionService ingestionService;
    private final Environment environment;

    public AgenticRagApplication(AgenticRagWorkflow workflow, IngestionService ingestionService, Environment environment) {
        this.workflow = workflow;
        this.ingestionService = ingestionService;
        this.environment = environment;
    }

    public static void main(String[] args) {
        SpringApplication.run(AgenticRagApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Hello from Agentic RAG!");

        // Skip ingestion during tests
        if (Arrays.asList(environment.getActiveProfiles()).contains("test")) {
            log.info("Test profile detected - skipping document ingestion");
            return;
        }

        // Uncomment to ingest documents (run once)
        //ingestDocuments();

        // Test the workflow
        String question = "How to make pizza?";
        log.info("Testing workflow with question: {}", question);

        GraphState result = workflow.invoke(question);

        log.info("\n=== FINAL RESULT ===");
        log.info("Question: {}", result.getQuestion());
        log.info("Answer: {}", result.getGeneration());
        log.info("===================\n");

    }

    private void ingestDocuments() {
        log.info("Starting document ingestion...");

        List<String> urls = Arrays.asList(
                "https://lilianweng.github.io/posts/2023-06-23-agent/",
                "https://lilianweng.github.io/posts/2023-03-15-prompt-engineering/",
                "https://lilianweng.github.io/posts/2023-10-25-adv-attack-llm/"
        );

        ingestionService.ingestUrls(urls);
        log.info("Document ingestion completed!");
    }
}
