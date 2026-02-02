package com.sachin.agentic.rag.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for LangSmith tracing
 */
@Configuration
public class LangSmithConfig {

    @Value("${langsmith.tracing.enabled:true}")
    private boolean tracingEnabled;

    @Value("${langsmith.api.key:}")
    private String apiKey;

    @Value("${langsmith.project:agentic-rag-java}")
    private String project;

    @Value("${langsmith.endpoint:https://api.smith.langchain.com}")
    private String endpoint;

    public boolean isTracingEnabled() {
        return tracingEnabled;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getProject() {
        return project;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public boolean isConfigured() {
        return tracingEnabled && apiKey != null && !apiKey.isEmpty();
    }
}
