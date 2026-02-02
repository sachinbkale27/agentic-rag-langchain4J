package com.sachin.agentic.rag.service;

import com.sachin.agentic.rag.config.LangSmithConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for sending traces to LangSmith
 */
@Service
public class LangSmithTracingService {
    private static final Logger log = LoggerFactory.getLogger(LangSmithTracingService.class);

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final LangSmithConfig config;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public LangSmithTracingService(LangSmithConfig config) {
        this.config = config;
        this.httpClient = new OkHttpClient();
        this.objectMapper = new ObjectMapper();

        if (config.isConfigured()) {
            log.info("LangSmith tracing enabled for project: {}", config.getProject());
        } else {
            log.info("LangSmith tracing disabled");
        }
    }

    public String startRun(String runType, String name, Map<String, Object> inputs) {
        return startRun(runType, name, inputs, null);
    }

    public String startRun(String runType, String name, Map<String, Object> inputs, String parentRunId) {
        if (!config.isConfigured()) {
            return null;
        }

        try {
            String runId = UUID.randomUUID().toString();

            // Build the run object according to LangSmith API schema
            Map<String, Object> run = new HashMap<>();
            run.put("id", runId);
            run.put("name", name);
            run.put("run_type", runType);
            run.put("inputs", inputs != null ? inputs : new HashMap<>());
            run.put("start_time", Instant.now().toString());
            run.put("execution_order", 1);

            // Add session information
            run.put("session_name", config.getProject());

            // Add extra metadata
            Map<String, Object> extra = new HashMap<>();
            extra.put("runtime", Map.of("platform", "java", "sdk", "custom"));
            run.put("extra", extra);

            if (parentRunId != null) {
                run.put("parent_run_id", parentRunId);
            }

            // Send the run object directly (not wrapped)
            sendToLangSmith(run, "runs");
            log.debug("Started LangSmith run: {} ({})", name, runId);
            return runId;
        } catch (Exception e) {
            log.warn("Failed to start LangSmith run: {}", e.getMessage(), e);
            return null;
        }
    }

    public void endRun(String runId, Map<String, Object> outputs, String error) {
        if (!config.isConfigured() || runId == null) {
            return;
        }

        try {
            Map<String, Object> update = new HashMap<>();
            update.put("end_time", Instant.now().toString());
            update.put("outputs", outputs != null ? outputs : new HashMap<>());

            if (error != null) {
                update.put("error", error);
            }

            // Send update directly (not wrapped)
            sendToLangSmith(update, "runs/" + runId);
            log.debug("Ended LangSmith run: {}", runId);
        } catch (Exception e) {
            log.warn("Failed to end LangSmith run: {}", e.getMessage(), e);
        }
    }

    public void logChain(String name, Map<String, Object> inputs, Map<String, Object> outputs) {
        logChain(name, inputs, outputs, null);
    }

    public void logChain(String name, Map<String, Object> inputs, Map<String, Object> outputs, String parentRunId) {
        String runId = startRun("chain", name, inputs, parentRunId);
        if (runId != null) {
            endRun(runId, outputs, null);
        }
    }

    public void logLLM(String name, Map<String, Object> inputs, Map<String, Object> outputs) {
        logLLM(name, inputs, outputs, null);
    }

    public void logLLM(String name, Map<String, Object> inputs, Map<String, Object> outputs, String parentRunId) {
        String runId = startRun("llm", name, inputs, parentRunId);
        if (runId != null) {
            endRun(runId, outputs, null);
        }
    }

    private void sendToLangSmith(Map<String, Object> data, String endpoint) {
        try {
            String url = config.getEndpoint() + "/" + endpoint;
            String jsonBody = objectMapper.writeValueAsString(data);

            log.debug("Sending to LangSmith URL: {}", url);
            log.debug("Payload: {}", jsonBody);

            // Use POST for creating runs, PATCH for updating
            Request.Builder requestBuilder = new Request.Builder()
                    .url(url)
                    .header("x-api-key", config.getApiKey())
                    .header("Content-Type", "application/json");

            // If endpoint contains a run ID (for updates), use PATCH, otherwise POST
            if (endpoint.contains("/")) {
                log.debug("Using PATCH method");
                requestBuilder.patch(RequestBody.create(jsonBody, JSON));
            } else {
                log.debug("Using POST method");
                requestBuilder.post(RequestBody.create(jsonBody, JSON));
            }

            Response response = httpClient.newCall(requestBuilder.build()).execute();
            String responseBody = response.body() != null ? response.body().string() : "";

            if (!response.isSuccessful()) {
                log.warn("LangSmith API error: {} - {}", response.code(), responseBody);
            } else {
                log.debug("Successfully sent to LangSmith: {}", response.code());
            }
            response.close();
        } catch (Exception e) {
            log.warn("Failed to send data to LangSmith: {}", e.getMessage(), e);
        }
    }
}
