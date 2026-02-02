package com.example.agentic.rag.model;

/**
 * Binary score for answer quality check
 */
public class GradeAnswer {
    /**
     * Answer addresses the question: true or false
     */
    private boolean binaryScore;

    public GradeAnswer() {
    }

    public GradeAnswer(boolean binaryScore) {
        this.binaryScore = binaryScore;
    }

    public boolean isBinaryScore() {
        return binaryScore;
    }

    public void setBinaryScore(boolean binaryScore) {
        this.binaryScore = binaryScore;
    }
}
