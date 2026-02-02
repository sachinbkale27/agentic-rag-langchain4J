package com.example.agentic.rag.model;

import dev.langchain4j.data.document.Document;

import java.util.List;

/**
 * Represents the state of the RAG graph
 */
public class GraphState {
    /**
     * User question
     */
    private String question;

    /**
     * LLM generation/answer
     */
    private String generation;

    /**
     * Whether to perform web search
     */
    private boolean webSearch;

    /**
     * List of retrieved documents
     */
    private List<Document> documents;

    public GraphState() {
    }

    public GraphState(String question, String generation, boolean webSearch, List<Document> documents) {
        this.question = question;
        this.generation = generation;
        this.webSearch = webSearch;
        this.documents = documents;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getGeneration() {
        return generation;
    }

    public void setGeneration(String generation) {
        this.generation = generation;
    }

    public boolean isWebSearch() {
        return webSearch;
    }

    public void setWebSearch(boolean webSearch) {
        this.webSearch = webSearch;
    }

    public List<Document> getDocuments() {
        return documents;
    }

    public void setDocuments(List<Document> documents) {
        this.documents = documents;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String question;
        private String generation;
        private boolean webSearch;
        private List<Document> documents;

        public Builder question(String question) {
            this.question = question;
            return this;
        }

        public Builder generation(String generation) {
            this.generation = generation;
            return this;
        }

        public Builder webSearch(boolean webSearch) {
            this.webSearch = webSearch;
            return this;
        }

        public Builder documents(List<Document> documents) {
            this.documents = documents;
            return this;
        }

        public GraphState build() {
            return new GraphState(question, generation, webSearch, documents);
        }
    }
}
