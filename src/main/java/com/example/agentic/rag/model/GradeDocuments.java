package com.example.agentic.rag.model;

/**
 * Binary score for relevance check on retrieved documents
 */
public class GradeDocuments {
    /**
     * Documents are relevant to the question: "yes" or "no"
     */
    private String binaryScore;

    public GradeDocuments() {
    }

    public GradeDocuments(String binaryScore) {
        this.binaryScore = binaryScore;
    }

    public String getBinaryScore() {
        return binaryScore;
    }

    public void setBinaryScore(String binaryScore) {
        this.binaryScore = binaryScore;
    }
}
