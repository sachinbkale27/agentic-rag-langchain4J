package com.sachin.agentic.rag.chain;

import com.sachin.agentic.rag.model.GradeAnswer;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Checks if the answer addresses the user question
 */
@Component
public class AnswerGrader {

    private final GraderService graderService;

    public AnswerGrader(@Value("${openai.api.key}") String apiKey) {
        ChatLanguageModel chatModel = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .temperature(0.0)
                .modelName("gpt-4")
                .build();

        this.graderService = AiServices.create(GraderService.class, chatModel);
    }

    public GradeAnswer grade(String question, String generation) {
        return graderService.gradeAnswer(question, generation);
    }

    interface GraderService {
        @dev.langchain4j.service.SystemMessage("""
                You are a grader assessing whether an answer addresses / resolves a question.
                Give a binary score true or false. True means that the answer resolves the question.
                """)
        @dev.langchain4j.service.UserMessage("""
                User question:
                {{question}}

                LLM generation: {{generation}}
                """)
        GradeAnswer gradeAnswer(@dev.langchain4j.service.V("question") String question,
                               @dev.langchain4j.service.V("generation") String generation);
    }
}
