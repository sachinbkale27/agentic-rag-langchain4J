package com.example.agentic.rag.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for interacting with the vector store (In-Memory)
 */
@Service
public class VectorStoreService {
    private static final Logger log = LoggerFactory.getLogger(VectorStoreService.class);

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;

    public VectorStoreService(@Value("${openai.api.key}") String apiKey) {
        // Using in-memory embedding store for development/testing
        this.embeddingStore = new InMemoryEmbeddingStore<>();

        this.embeddingModel = OpenAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName("text-embedding-ada-002")
                .build();
    }

    public List<Document> retrieveDocuments(String query) {
        return retrieveDocuments(query, 4);
    }

    public List<Document> retrieveDocuments(String query, int maxResults) {
        log.info("Retrieving documents for query: {}", query);

        // Embed the query
        Embedding queryEmbedding = embeddingModel.embed(query).content();

        // Search for similar documents
        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(maxResults)
                .minScore(0.0)
                .build();

        EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);

        // Convert results to documents
        return searchResult.matches().stream()
                .map(EmbeddingMatch::embedded)
                .map(segment -> Document.from(segment.text(), segment.metadata()))
                .collect(Collectors.toList());
    }
}
