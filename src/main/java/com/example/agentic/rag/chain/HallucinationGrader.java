package com.example.agentic.rag.chain;

import com.example.agentic.rag.model.GradeHallucinations;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Checks if the LLM generation is grounded in the retrieved documents
 */
@Component
public class HallucinationGrader {

    private final GraderService graderService;

    public HallucinationGrader(@Value("${openai.api.key}") String apiKey) {
        ChatLanguageModel chatModel = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .temperature(0.0)
                .modelName("gpt-4")
                .build();

        this.graderService = AiServices.create(GraderService.class, chatModel);
    }

    public GradeHallucinations grade(String documents, String generation) {
        return graderService.gradeHallucination(documents, generation);
    }

    interface GraderService {
        @dev.langchain4j.service.SystemMessage("""
                You are a grader assessing whether an LLM generation is grounded in / supported by a set of retrieved facts.
                Give a binary score true or false. True means that the answer is grounded in / supported by the set of facts.
                """)
        @dev.langchain4j.service.UserMessage("""
                Set of facts:
                {{documents}}

                LLM generation: {{generation}}
                """)
        GradeHallucinations gradeHallucination(@dev.langchain4j.service.V("documents") String documents,
                                               @dev.langchain4j.service.V("generation") String generation);
    }
}
