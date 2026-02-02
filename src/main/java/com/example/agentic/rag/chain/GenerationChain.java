package com.example.agentic.rag.chain;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Generates answers based on context and question
 */
@Component
public class GenerationChain {

    private final GeneratorService generatorService;

    public GenerationChain(@Value("${openai.api.key}") String apiKey) {
        ChatLanguageModel chatModel = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .temperature(0.0)
                .modelName("gpt-4")
                .build();

        this.generatorService = AiServices.create(GeneratorService.class, chatModel);
    }

    public String generate(String context, String question) {
        return generatorService.generateAnswer(context, question);
    }

    interface GeneratorService {
        @dev.langchain4j.service.SystemMessage("""
                You are an assistant for question-answering tasks.
                Use the following pieces of retrieved context to answer the question.
                If you don't know the answer, just say that you don't know.
                Use three sentences maximum and keep the answer concise.
                """)
        @dev.langchain4j.service.UserMessage("""
                Question: {{question}}
                Context: {{context}}
                Answer:
                """)
        String generateAnswer(@dev.langchain4j.service.V("context") String context,
                            @dev.langchain4j.service.V("question") String question);
    }
}
