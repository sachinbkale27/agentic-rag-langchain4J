package com.example.agentic.rag.chain;

import com.example.agentic.rag.model.RouteQuery;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Routes user questions to the appropriate datasource (vectorstore or web_search)
 */
@Component
public class QuestionRouter {

    private final RouterService routerService;

    public QuestionRouter(@Value("${openai.api.key}") String apiKey) {
        ChatLanguageModel chatModel = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .temperature(0.0)
                .modelName("gpt-4")
                .build();

        this.routerService = AiServices.create(RouterService.class, chatModel);
    }

    public RouteQuery route(String question) {
        return routerService.routeQuestion(question);
    }

    interface RouterService {
        @dev.langchain4j.service.SystemMessage("""
                You are an expert at routing a user question to a vectorstore or web_search.
                The vectorstore contains documents related to agents, prompt engineering and adversarial attacks.
                Use the vectorstore for questions on these topics. For everything else, use web_search.
                Return either 'vectorstore' or 'web_search' as the datasource.
                """)
        @dev.langchain4j.service.UserMessage("Question: {{question}}")
        RouteQuery routeQuestion(@dev.langchain4j.service.V("question") String question);
    }
}
