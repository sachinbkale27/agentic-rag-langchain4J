package com.example.agentic.rag.model;

/**
 * Binary score for hallucination check
 */
public class GradeHallucinations {
    /**
     * Answer is grounded in facts: true or false
     */
    private boolean binaryScore;

    public GradeHallucinations() {
    }

    public GradeHallucinations(boolean binaryScore) {
        this.binaryScore = binaryScore;
    }

    public boolean isBinaryScore() {
        return binaryScore;
    }

    public void setBinaryScore(boolean binaryScore) {
        this.binaryScore = binaryScore;
    }
}
