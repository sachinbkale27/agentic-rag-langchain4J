# LangSmith Integration Guide

This guide explains how to use LangSmith tracing with your Java Agentic RAG application.

## What is LangSmith?

LangSmith is a platform for monitoring, debugging, and improving LLM applications. It provides:
- **Tracing**: Track every step of your agentic workflow
- **Monitoring**: Monitor performance and costs
- **Debugging**: Identify issues in your RAG pipeline
- **Evaluation**: Test and improve your application

## Setup

### 1. Get LangSmith API Key

1. Go to [https://smith.langchain.com](https://smith.langchain.com)
2. Sign up or log in
3. Navigate to Settings → API Keys
4. Create a new API key

### 2. Configure Environment Variables

Add these to your `.env` file or export them:

```bash
# Enable tracing
export LANGSMITH_TRACING_V2=true

# Your LangSmith API key
export LANGSMITH_API_KEY=lsv2_pt_your_key_here

# Project name (will be created automatically if it doesn't exist)
export LANGSMITH_PROJECT=agentic-rag-java
```

Or add to your `application.properties`:

```properties
langsmith.tracing.enabled=true
langsmith.api.key=lsv2_pt_your_key_here
langsmith.project=agentic-rag-java
```

### 3. Run Your Application

```bash
# Export environment variables
export LANGSMITH_TRACING_V2=true
export LANGSMITH_API_KEY=lsv2_pt_your_key_here
export LANGSMITH_PROJECT=agentic-rag-java

# Run the application
make run
```

## What Gets Traced

The application automatically traces:

### 1. Workflow Level
- **AgenticRAGWorkflow**: The entire workflow from question to answer
  - Input: Question
  - Output: Answer, document count
  - Duration: Total workflow time

### 2. Routing
- **RouteQuestion**: Question routing decisions
  - Input: Question
  - Output: Datasource (vectorstore or web_search)

### 3. LLM Calls (via QuestionRouter, Graders, etc.)
- All OpenAI API calls are automatically tracked
- Token usage
- Latency
- Model used

## Viewing Traces in LangSmith

1. Go to [https://smith.langchain.com](https://smith.langchain.com)
2. Select your project (e.g., "agentic-rag-java")
3. View traces in the "Traces" tab

### Trace Hierarchy

```
AgenticRAGWorkflow
├── RouteQuestion
│   └── LLM Call (OpenAI)
├── Retrieve (if vectorstore)
├── GradeDocuments
│   └── LLM Call (OpenAI)
├── Generate
│   └── LLM Call (OpenAI)
├── HallucinationGrader
│   └── LLM Call (OpenAI)
└── AnswerGrader
    └── LLM Call (OpenAI)
```

## Adding Custom Traces

You can add custom tracing to your own code:

### Example: Tracing a Custom Chain

```java
@Service
@RequiredArgsConstructor
public class MyCustomService {

    private final LangSmithTracingService tracingService;

    public String processData(String input) {
        // Start trace
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("input", input);
        String runId = tracingService.startRun("chain", "MyCustomProcess", inputs);

        try {
            // Your logic here
            String result = doSomething(input);

            // End trace with success
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("result", result);
            tracingService.endRun(runId, outputs, null);

            return result;
        } catch (Exception e) {
            // End trace with error
            tracingService.endRun(runId, null, e.getMessage());
            throw e;
        }
    }
}
```

### Example: Tracing LLM Calls

```java
Map<String, Object> inputs = new HashMap<>();
inputs.put("prompt", prompt);

Map<String, Object> outputs = new HashMap<>();
outputs.put("response", response);

tracingService.logLLM("CustomLLMCall", inputs, outputs);
```

## Configuration Options

### application.properties

```properties
# Enable/disable tracing (default: false)
langsmith.tracing.enabled=true

# LangSmith API key (required if tracing enabled)
langsmith.api.key=${LANGSMITH_API_KEY}

# Project name (default: agentic-rag-java)
langsmith.project=${LANGSMITH_PROJECT:agentic-rag-java}

# LangSmith API endpoint (default: https://api.smith.langchain.com)
langsmith.endpoint=${LANGSMITH_ENDPOINT:https://api.smith.langchain.com}
```

## Debugging

### Enable Debug Logging

Add to `application.properties`:

```properties
logging.level.service.com.example.agentic.rag.LangSmithTracingService=DEBUG
```

This will show trace events in your logs:

```
DEBUG - Started LangSmith run: AgenticRAGWorkflow (uuid-here)
DEBUG - Ended LangSmith run: uuid-here
```

### Common Issues

#### 1. Traces Not Appearing

**Check:**
- Is `LANGSMITH_TRACING_V2=true`?
- Is `LANGSMITH_API_KEY` set correctly?
- Is your API key valid?
- Check logs for errors

**Solution:**
```bash
# Verify environment variables
echo $LANGSMITH_TRACING_V2
echo $LANGSMITH_API_KEY
echo $LANGSMITH_PROJECT

# Check application logs
make run 2>&1 | grep -i langsmith
```

#### 2. Authentication Error

**Error:** `401 Unauthorized`

**Solution:**
- Verify your API key is correct
- Regenerate API key in LangSmith dashboard

#### 3. Tracing Disabled

If you see "LangSmith tracing disabled" in logs:

**Check:**
- `langsmith.tracing.enabled=true` in properties
- `LANGSMITH_TRACING_V2=true` in environment

## Performance Impact

LangSmith tracing has minimal performance impact:
- Async HTTP calls to LangSmith API
- Failed traces don't affect your application
- Traces are batched when possible

## Best Practices

1. **Use Descriptive Names**: Name your traces clearly
   ```java
   tracingService.startRun("chain", "DocumentRetrievalWithFilter", inputs);
   ```

2. **Add Metadata**: Include relevant context
   ```java
   inputs.put("filter_type", "date_range");
   inputs.put("max_results", 10);
   ```

3. **Trace Decision Points**: Track important decisions
   ```java
   outputs.put("decision", "use_web_search");
   outputs.put("reason", "no_relevant_docs_found");
   ```

4. **Handle Errors**: Always trace errors
   ```java
   tracingService.endRun(runId, null, exception.getMessage());
   ```

## Disabling Tracing

To disable tracing:

```bash
export LANGSMITH_TRACING_V2=false
# or simply don't set it
```

Or in `application.properties`:

```properties
langsmith.tracing.enabled=false
```

## Next Steps

- Explore traces in LangSmith dashboard
- Set up alerts for errors
- Create datasets for evaluation
- Compare different prompts and models

## Resources

- [LangSmith Documentation](https://docs.smith.langchain.com)
- [LangSmith Python SDK](https://github.com/langchain-ai/langsmith-sdk)
- [LangChain4j Documentation](https://docs.langchain4j.dev)
