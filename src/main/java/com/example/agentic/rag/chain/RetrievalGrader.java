package com.example.agentic.rag.chain;

import com.example.agentic.rag.model.GradeDocuments;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Grades the relevance of retrieved documents to the user question
 */
@Component
public class RetrievalGrader {

    private final GraderService graderService;

    public RetrievalGrader(@Value("${openai.api.key}") String apiKey) {
        ChatLanguageModel chatModel = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .temperature(0.0)
                .modelName("gpt-4")
                .build();

        this.graderService = AiServices.create(GraderService.class, chatModel);
    }

    public GradeDocuments grade(String document, String question) {
        return graderService.gradeDocument(document, question);
    }

    interface GraderService {
        @dev.langchain4j.service.SystemMessage("""
                You are a grader assessing relevance of a retrieved document to user question.
                If the document contains keyword(s) or semantic meaning related to the question, grade its relevance.
                Give a binary score 'yes' or 'no' to indicate whether the document is relevant to the question.
                """)
        @dev.langchain4j.service.UserMessage("""
                Retrieved document:
                {{document}}

                User question: {{question}}
                """)
        GradeDocuments gradeDocument(@dev.langchain4j.service.V("document") String document,
                                    @dev.langchain4j.service.V("question") String question);
    }
}
