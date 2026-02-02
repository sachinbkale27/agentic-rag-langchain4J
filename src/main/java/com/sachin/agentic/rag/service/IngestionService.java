package com.sachin.agentic.rag.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.UrlDocumentLoader;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for ingesting documents into the vector store
 */
@Service
public class IngestionService {
    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;
    private final DocumentSplitter documentSplitter;
    private final DocumentParser documentParser;

    public IngestionService(@Value("${openai.api.key}") String apiKey) {
        // Using in-memory embedding store for development/testing
        this.embeddingStore = new InMemoryEmbeddingStore<>();

        this.embeddingModel = OpenAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName("text-embedding-ada-002")
                .build();

        this.documentSplitter = DocumentSplitters.recursive(250, 0);
        this.documentParser = new ApacheTikaDocumentParser();
    }

    public void ingestUrls(List<String> urls) {
        log.info("Starting ingestion of {} URLs", urls.size());

        List<Document> allDocuments = new ArrayList<>();

        for (String urlString : urls) {
            try {
                Document document = UrlDocumentLoader.load(urlString, documentParser);
                allDocuments.add(document);
                log.info("Loaded document from: {}", urlString);
            } catch (Exception e) {
                log.error("Error loading document from URL: {}", urlString, e);
            }
        }

        log.info("Total documents loaded: {}", allDocuments.size());

        // Split documents
        List<TextSegment> segments = new ArrayList<>();
        for (Document doc : allDocuments) {
            segments.addAll(documentSplitter.split(doc));
        }

        log.info("Total segments after splitting: {}", segments.size());

        // Embed and store segments
        for (TextSegment segment : segments) {
            Embedding embedding = embeddingModel.embed(segment).content();
            embeddingStore.add(embedding, segment);
        }

        log.info("Ingestion completed. Stored {} segments", segments.size());
    }
}
