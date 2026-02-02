package com.example.agentic.rag.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Service for performing web searches using Tavily API
 */
@Service
public class TavilySearchService {
    private static final Logger log = LoggerFactory.getLogger(TavilySearchService.class);

    private static final String TAVILY_API_URL = "https://api.tavily.com/search";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public TavilySearchService(@Value("${tavily.api.key}") String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public String search(String query) {
        return search(query, 3);
    }

    public String search(String query, int maxResults) {
        log.info("Performing web search for: {}", query);

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("api_key", apiKey);
            requestBody.put("query", query);
            requestBody.put("max_results", maxResults);

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            Request request = new Request.Builder()
                    .url(TAVILY_API_URL)
                    .post(RequestBody.create(jsonBody, JSON))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected response code: " + response);
                }

                String responseBody = response.body().string();
                JsonNode jsonResponse = objectMapper.readTree(responseBody);
                JsonNode results = jsonResponse.get("results");

                if (results != null && results.isArray()) {
                    return StreamSupport.stream(results.spliterator(), false)
                            .map(result -> result.get("content").asText())
                            .collect(Collectors.joining(" "));
                }

                return "";
            }
        } catch (IOException e) {
            log.error("Error performing web search", e);
            return "";
        }
    }
}
