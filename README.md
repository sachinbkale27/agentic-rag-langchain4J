# Agentic RAG with LangChain4j and LangGraph

A sophisticated Retrieval-Augmented Generation (RAG) application built with Java, implementing **LangGraph** concepts using **LangChain4j**. This application demonstrates an agentic workflow with self-reflection, routing, and fallback mechanisms for robust question answering.

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [LangGraph Concepts in Java](#langgraph-concepts-in-java)
- [Workflow Components](#workflow-components)
- [Setup](#setup)
- [Usage](#usage)
- [LangSmith Tracing](#langsmith-tracing)
- [Project Structure](#project-structure)
- [Spring AI vs LangChain4j](#spring-ai-vs-langchain4j)

## Overview

This application implements an **Agentic RAG** pattern inspired by LangGraph's state-based workflow design. Unlike traditional RAG systems that simply retrieve and generate, this system:

- **Routes queries** intelligently between vector store and web search
- **Grades retrieved documents** for relevance
- **Validates generations** for hallucinations and accuracy
- **Self-corrects** by re-routing to web search when needed
- **Traces all operations** to LangSmith for observability

### Key Features

- 🔄 **Adaptive Workflow**: Dynamic routing based on query type
- 📊 **Quality Control**: Multiple grading steps ensure accurate answers
- 🌐 **Fallback Mechanisms**: Web search when vector store is insufficient
- 📈 **Full Observability**: LangSmith integration for tracing
- 🧠 **Self-Reflection**: Checks for hallucinations and answer quality

## Architecture

### LangGraph State Machine Pattern

While LangChain4j doesn't have a direct LangGraph port, this application implements the core concepts:

```
┌─────────────────────────────────────────────────────────────┐
│                    AgenticRagWorkflow                        │
│                    (State Machine)                           │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │  Route Question  │
                    └──────────────────┘
                       /            \
                      /              \
            ┌─────────┐              ┌──────────┐
            │ Vector  │              │   Web    │
            │  Store  │              │  Search  │
            └─────────┘              └──────────┘
                 │                         │
                 ▼                         │
        ┌─────────────────┐               │
        │ Grade Documents │               │
        └─────────────────┘               │
                 │                         │
                 ▼                         │
        ┌─────────────────┐               │
        │ Decide: Generate│               │
        │  or Web Search? │               │
        └─────────────────┘               │
                 │                         │
                 └──────────┬──────────────┘
                            ▼
                   ┌─────────────────┐
                   │    Generate     │
                   │     Answer      │
                   └─────────────────┘
                            │
                            ▼
                   ┌─────────────────┐
                   │ Check Quality   │
                   │ - Hallucination │
                   │ - Relevance     │
                   └─────────────────┘
                            │
                            ▼
                   ┌─────────────────┐
                   │  Final Answer   │
                   └─────────────────┘
```

### State Management

The workflow maintains state using the `GraphState` class (analogous to LangGraph's state):

```java
@Builder
public class GraphState {
    private String question;          // User's question
    private List<Document> documents; // Retrieved/graded documents
    private String generation;        // Generated answer
    private boolean webSearch;        // Flag to trigger web search
}
```

State flows through the workflow, being transformed by each node, just like in LangGraph.

## LangGraph Concepts in Java

### 1. **Nodes** → Spring Components

Each LangGraph "node" is implemented as a Spring `@Component`:

| LangGraph Concept | Java Implementation | Purpose |
|-------------------|---------------------|---------|
| `@node` decorator | `@Component` class | Workflow nodes |
| State parameter | `GraphState` object | Passes data between nodes |
| Node function | `public GraphState method()` | Transforms state |

**Example:**

```java
@Component
public class RetrieveNode {
    public GraphState retrieve(GraphState state) {
        // Transform state: add documents
        List<Document> documents = vectorStore.retrieve(state.getQuestion());
        return state.withDocuments(documents);
    }
}
```

### 2. **Conditional Edges** → Decision Methods

LangGraph's conditional routing is implemented as decision methods:

```java
private String routeQuestion(GraphState state) {
    RouteQuery route = questionRouter.route(state.getQuestion());

    if (WEBSEARCH.equals(route.getDatasource())) {
        return WEBSEARCH;  // Route to web search
    } else {
        return RETRIEVE;   // Route to vector store
    }
}
```

### 3. **Workflow Orchestration** → `AgenticRagWorkflow`

The main workflow class orchestrates the graph execution:

```java
@Component
public class AgenticRagWorkflow {
    public GraphState invoke(String question) {
        GraphState state = GraphState.builder()
            .question(question)
            .build();

        // Execute nodes in sequence, branching based on conditions
        String route = routeQuestion(state);

        if (WEBSEARCH.equals(route)) {
            state = webSearchNode.webSearch(state);
        } else {
            state = retrieveNode.retrieve(state);
            state = gradeDocumentsNode.gradeDocuments(state);

            if (state.isWebSearch()) {
                state = webSearchNode.webSearch(state);
            }
        }

        state = generateNode.generate(state);
        state = checkGenerationQuality(state);

        return state;
    }
}
```

### 4. **Chains** → LangChain4j `AiServices`

LangChain4j's `AiServices` is used to create structured LLM chains:

```java
@Component
public class GenerationChain {
    interface GeneratorService {
        @SystemMessage("You are an assistant for question-answering tasks...")
        @UserMessage("Question: {{question}}\nContext: {{context}}\nAnswer:")
        String generateAnswer(@V("context") String context,
                            @V("question") String question);
    }

    private final GeneratorService service;

    public GenerationChain(String apiKey) {
        ChatLanguageModel model = OpenAiChatModel.builder()
            .apiKey(apiKey)
            .modelName("gpt-4")
            .build();

        this.service = AiServices.create(GeneratorService.class, model);
    }
}
```

## Workflow Components

### Nodes

#### 1. **RetrieveNode**
- **Purpose**: Retrieves relevant documents from vector store
- **Input**: Question
- **Output**: List of documents
- **LangChain4j Component**: `EmbeddingStore`, `EmbeddingModel`

```java
@Component
public class RetrieveNode {
    private final VectorStoreService vectorStoreService;

    public GraphState retrieve(GraphState state) {
        List<Document> documents =
            vectorStoreService.retrieveDocuments(state.getQuestion());
        return state.withDocuments(documents);
    }
}
```

#### 2. **GradeDocumentsNode**
- **Purpose**: Grades each document for relevance to the question
- **Uses**: LLM-based grading with structured output
- **Self-Reflection**: Filters out irrelevant documents

```java
@Component
public class GradeDocumentsNode {
    private final RetrievalGrader retrievalGrader;

    public GraphState gradeDocuments(GraphState state) {
        List<Document> filteredDocs = new ArrayList<>();
        boolean needsWebSearch = false;

        for (Document doc : state.getDocuments()) {
            GradeDocuments score = retrievalGrader.grade(doc.text(),
                                                         state.getQuestion());
            if ("yes".equalsIgnoreCase(score.getBinaryScore())) {
                filteredDocs.add(doc);
            } else {
                needsWebSearch = true; // Trigger fallback
            }
        }

        return state.withDocuments(filteredDocs)
                   .withWebSearch(needsWebSearch);
    }
}
```

#### 3. **GenerateNode**
- **Purpose**: Generates answer using LLM with retrieved context
- **Chain**: `GenerationChain` with structured prompts
- **LangChain4j Features**: `@SystemMessage`, `@UserMessage`, `@V` variables

```java
@Component
public class GenerateNode {
    private final GenerationChain generationChain;

    public GraphState generate(GraphState state) {
        String context = state.getDocuments()
            .stream()
            .map(Document::text)
            .collect(Collectors.joining("\n\n"));

        String answer = generationChain.generate(context,
                                                state.getQuestion());

        return state.withGeneration(answer);
    }
}
```

#### 4. **WebSearchNode**
- **Purpose**: Fallback mechanism using web search
- **Integration**: Tavily Search API
- **When Triggered**: Insufficient or irrelevant vector store results

```java
@Component
public class WebSearchNode {
    private final TavilySearchService tavilyService;

    public GraphState webSearch(GraphState state) {
        String searchResults = tavilyService.search(state.getQuestion());
        Document webDoc = Document.from(searchResults);

        List<Document> allDocs = new ArrayList<>(state.getDocuments());
        allDocs.add(webDoc);

        return state.withDocuments(allDocs);
    }
}
```

### Chains (LLM Operations)

#### 1. **QuestionRouter**
- **Purpose**: Routes questions to appropriate data source
- **Output**: Structured JSON: `{"datasource": "vectorstore" | "websearch"}`
- **LangChain4j Feature**: Structured output parsing

```java
interface RouterService {
    @SystemMessage("Route the question to vectorstore or websearch...")
    @UserMessage("Question: {{question}}")
    RouteQuery route(@V("question") String question);
}
```

#### 2. **RetrievalGrader**
- **Purpose**: Binary classification of document relevance
- **Output**: `{"binary_score": "yes" | "no"}`

#### 3. **HallucinationGrader**
- **Purpose**: Validates generation is grounded in documents
- **Self-Reflection**: Detects hallucinations

#### 4. **AnswerGrader**
- **Purpose**: Checks if answer addresses the question
- **Self-Reflection**: Triggers re-generation if needed

### Decision Logic (Conditional Edges)

```java
private String routeQuestion(GraphState state) {
    // Conditional routing logic
    RouteQuery route = questionRouter.route(state.getQuestion());
    return route.getDatasource();
}

private String decideToGenerate(GraphState state) {
    // Check if we have enough relevant documents
    return state.isWebSearch() ? WEBSEARCH : GENERATE;
}

private GraphState checkGenerationQuality(GraphState state) {
    // Hallucination check
    if (!hallucinationGrader.grade(...).isBinaryScore()) {
        return generateNode.generate(state); // Re-generate
    }

    // Answer relevance check
    if (!answerGrader.grade(...).isBinaryScore()) {
        state = webSearchNode.webSearch(state);
        return generateNode.generate(state); // Search + re-generate
    }

    return state; // Success!
}
```

## Setup

### Prerequisites

- Java 21+
- Maven 3.9+
- OpenAI API key
- Tavily API key (for web search)
- LangSmith API key (optional, for tracing)

### Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd agentic-rag
   ```

2. **Set up environment variables**

   Create a `.env` file or export variables:
   ```bash
   export OPENAI_API_KEY=your_openai_key
   export TAVILY_API_KEY=your_tavily_key
   export LANGSMITH_TRACING_V2=true
   export LANGSMITH_API_KEY=your_langsmith_key
   export LANGSMITH_PROJECT=java-agentic-rag
   ```

3. **Build the project**
   ```bash
   mvn clean install
   ```

### Configuration

Edit `src/main/resources/application.properties`:

```properties
# OpenAI Configuration
openai.api.key=${OPENAI_API_KEY}

# Tavily Configuration
tavily.api.key=${TAVILY_API_KEY}

# LangSmith Configuration (Optional)
langsmith.tracing.enabled=${LANGSMITH_TRACING_V2:false}
langsmith.api.key=${LANGSMITH_API_KEY:}
langsmith.project=${LANGSMITH_PROJECT:java-agentic-rag}
```

## Usage

### 1. Document Ingestion

First, ingest documents into the vector store:

```java
// In AgenticRagApplication.java
private void ingestDocuments() {
    List<String> urls = Arrays.asList(
        "https://lilianweng.github.io/posts/2023-06-23-agent/",
        "https://lilianweng.github.io/posts/2023-03-15-prompt-engineering/"
    );
    ingestionService.ingestUrls(urls);
}
```

Run with ingestion enabled:
```bash
mvn spring-boot:run
```

### 2. Query the Workflow

After ingestion, query the system:

```java
String question = "What is prompt engineering?";
GraphState result = workflow.invoke(question);

System.out.println("Answer: " + result.getGeneration());
```

### 3. Example Workflow Execution

**Question**: "How to make pizza?"

```
1. RouteQuestion → WEBSEARCH (not in vector store)
2. WebSearchNode → Retrieve from Tavily
3. GenerateNode → Create answer from web results
4. CheckQuality → Validate (no hallucinations, addresses question)
5. Return answer
```

**Question**: "What are LLM agents?"

```
1. RouteQuestion → VECTORSTORE (in our ingested docs)
2. RetrieveNode → Get relevant documents
3. GradeDocuments → All relevant (no web search needed)
4. GenerateNode → Create answer from docs
5. CheckQuality → Validate
6. Return answer
```

## LangSmith Tracing

### Overview

LangSmith integration provides full observability into the agentic workflow:

- **Run Trees**: Hierarchical view of all operations
- **Timing**: Performance metrics for each node
- **Inputs/Outputs**: Complete data flow visibility
- **Debugging**: Identify bottlenecks and failures

### Trace Structure

```
AgenticRAGWorkflow (chain)
├── RouteQuestion (chain)
│   └── Inputs: {"question": "..."}
│   └── Outputs: {"datasource": "vectorstore"}
├── RetrieveDocuments (retriever)
│   └── Inputs: {"question": "..."}
│   └── Outputs: {"num_documents": 4, "documents": [...]}
├── GradeDocuments (chain)
│   └── Inputs: {"question": "...", "num_documents": 4}
│   └── Outputs: {"relevant_documents": 3, "needs_web_search": false}
└── GenerateAnswer (llm)
    └── Inputs: {"question": "...", "context": "...", "num_documents": 3}
    └── Outputs: {"generation": "LLM agents are..."}
```

### Viewing Traces

1. Go to https://smith.langchain.com/
2. Navigate to your project: `java-agentic-rag`
3. View complete execution traces with timing and data flow

### Custom Tracing

Each node automatically logs to LangSmith:

```java
@Component
public class GenerateNode {
    private final LangSmithTracingService tracingService;

    public GraphState generate(GraphState state) {
        // Start trace
        Map<String, Object> inputs = Map.of(
            "question", state.getQuestion(),
            "num_documents", state.getDocuments().size()
        );
        String runId = tracingService.startRun("llm", "GenerateAnswer", inputs);

        // Execute operation
        String answer = generationChain.generate(...);

        // End trace with outputs
        Map<String, Object> outputs = Map.of("generation", answer);
        tracingService.endRun(runId, outputs, null);

        return state.withGeneration(answer);
    }
}
```

## Project Structure

```
src/main/java/com/example/agentic/rag/
├── AgenticRagApplication.java          # Main application entry point
│
├── graph/
│   └── AgenticRagWorkflow.java         # State machine orchestrator (LangGraph)
│
├── node/                                # Workflow nodes
│   ├── RetrieveNode.java               # Vector store retrieval
│   ├── GradeDocumentsNode.java         # Document relevance grading
│   ├── GenerateNode.java               # Answer generation
│   └── WebSearchNode.java              # Web search fallback
│
├── chain/                               # LLM chains (LangChain4j AiServices)
│   ├── QuestionRouter.java             # Query routing
│   ├── GenerationChain.java            # Answer generation
│   ├── RetrievalGrader.java            # Document grading
│   ├── HallucinationGrader.java        # Hallucination detection
│   └── AnswerGrader.java               # Answer quality check
│
├── model/                               # State and data models
│   ├── GraphState.java                 # Workflow state (like LangGraph state)
│   ├── GraphConstants.java             # Node/edge constants
│   ├── RouteQuery.java                 # Routing decision model
│   ├── GradeDocuments.java             # Grading result model
│   ├── GradeHallucinations.java        # Hallucination check model
│   └── GradeAnswer.java                # Answer quality model
│
├── service/                             # Business logic services
│   ├── VectorStoreService.java         # Vector store operations
│   ├── IngestionService.java           # Document ingestion
│   ├── TavilySearchService.java        # Web search integration
│   └── LangSmithTracingService.java    # Observability
│
└── config/
    └── LangSmithConfig.java             # LangSmith configuration
```

## Key Patterns and Best Practices

### 1. **Immutable State**

State is transformed through the workflow, not mutated:

```java
// Good: Return new state
return GraphState.builder()
    .question(state.getQuestion())
    .documents(newDocuments)
    .build();

// Avoid: Mutating state
state.setDocuments(newDocuments);
```

### 2. **Dependency Injection**

All components use Spring's DI for loose coupling:

```java
@Component
public class RetrieveNode {
    private final VectorStoreService vectorStore;
    private final LangSmithTracingService tracing;

    // Constructor injection
    public RetrieveNode(VectorStoreService vectorStore,
                       LangSmithTracingService tracing) {
        this.vectorStore = vectorStore;
        this.tracing = tracing;
    }
}
```

### 3. **Structured Outputs**

Use LangChain4j's structured output parsing:

```java
interface GraderService {
    @SystemMessage("Grade the document...")
    @UserMessage("Document: {{document}}\nQuestion: {{question}}")
    GradeDocuments grade(@V("document") String doc,
                        @V("question") String question);
}

// GradeDocuments is a POJO that gets automatically parsed
@Data
public class GradeDocuments {
    private String binaryScore; // "yes" or "no"
}
```

### 4. **Error Handling and Fallbacks**

The workflow includes multiple fallback mechanisms:

- Irrelevant documents → Trigger web search
- Hallucinated answer → Regenerate
- Poor answer quality → Web search + regenerate

## Comparison: LangGraph (Python) vs This Implementation (Java)

| Feature | LangGraph (Python) | This Implementation (Java) |
|---------|-------------------|----------------------------|
| State Management | `TypedDict` | `@Builder` class |
| Nodes | `@node` decorator | Spring `@Component` |
| Edges | `add_edge()` | Method calls in orchestrator |
| Conditional Edges | `add_conditional_edges()` | Decision methods |
| Workflow Execution | `graph.compile()` → `invoke()` | `workflow.invoke()` |
| LLM Integration | LangChain LCEL | LangChain4j `AiServices` |
| Tracing | Built-in LangSmith | Custom `LangSmithTracingService` |

## Advanced Topics

### Custom Node Creation

To add a new node to the workflow:

1. **Create a Node Component**
   ```java
   @Component
   public class CustomNode {
       public GraphState process(GraphState state) {
           // Your logic here
           return state.withNewData(...);
       }
   }
   ```

2. **Add to Workflow**
   ```java
   @Component
   public class AgenticRagWorkflow {
       private final CustomNode customNode;

       public GraphState invoke(String question) {
           // ... existing flow
           state = customNode.process(state);
           // ... continue flow
       }
   }
   ```

3. **Add Tracing**
   ```java
   String runId = tracingService.startRun("chain", "CustomNode", inputs);
   // ... execute
   tracingService.endRun(runId, outputs, null);
   ```

### Extending State

To add new state fields:

```java
@Builder
@Data
public class GraphState {
    private String question;
    private List<Document> documents;
    private String generation;
    private boolean webSearch;

    // New field
    private Map<String, Object> metadata;
}
```

## Troubleshooting

### Common Issues

1. **Missing Dependencies Error**
   ```
   NoClassDefFoundError: org/apache/commons/io/input/ChecksumInputStream
   ```
   **Solution**: Ensure compatible versions:
   - `commons-io: 2.11.0`
   - `commons-compress: 1.21`

2. **Empty LangSmith Traces**
   ```
   LangSmith tracing disabled
   ```
   **Solution**: Set environment variables:
   ```bash
   export LANGSMITH_TRACING_V2=true
   export LANGSMITH_API_KEY=your_key
   ```

3. **API Key Errors**
   ```
   Incorrect API key provided
   ```
   **Solution**: Verify `.env` file or environment variables are set correctly

## Spring AI vs LangChain4j

This project uses **LangChain4j**, but you might wonder when to use Spring AI instead. Here's a quick guide:

### Choose Spring AI When:
- ✅ Building a Spring Boot application (native integration)
- ✅ You prefer convention over configuration
- ✅ Team is familiar with Spring patterns
- ✅ Need enterprise Spring features (Security, Cloud, etc.)
- ✅ Want auto-configuration and minimal boilerplate

### Choose LangChain4j When:
- ✅ Need framework independence (works anywhere)
- ✅ Team knows Python LangChain (direct concept mapping)
- ✅ Want explicit control over configuration
- ✅ Building non-Spring applications (CLI, desktop, Android)
- ✅ Need advanced features (structured outputs, complex memory, tool calling)
- ✅ Working in polyglot environment

### Quick Comparison

| Aspect | Spring AI | LangChain4j |
|--------|-----------|-------------|
| **Integration** | Native Spring Boot | Framework-agnostic |
| **Configuration** | Declarative (YAML) | Programmatic (Builders) |
| **Learning Curve** | Easy for Spring devs | Easy for LangChain users |
| **Control** | Convention-based | Explicit |
| **Best For** | Spring Boot apps | Any Java project |

**📖 For a detailed comparison**, see [SPRING_AI_VS_LANGCHAIN4J.md](./SPRING_AI_VS_LANGCHAIN4J.md) which covers:
- Detailed feature comparison
- Code examples for both frameworks
- Migration considerations
- Decision matrix
- When to use hybrid approach

## Contributing

Contributions are welcome! Areas for improvement:

- [ ] Add support for more vector stores (Pinecone, Weaviate)
- [ ] Implement caching for repeated queries
- [ ] Add streaming responses
- [ ] Create web UI
- [ ] Add more sophisticated routing logic
- [ ] Implement multi-query retrieval

## License

[Your License Here]

## Acknowledgments

- Inspired by [LangGraph](https://github.com/langchain-ai/langgraph) concepts
- Built with [LangChain4j](https://github.com/langchain4j/langchain4j)
- Based on [Agentic RAG patterns](https://lilianweng.github.io/posts/2023-06-23-agent/)

## Resources

- [LangChain4j Documentation](https://docs.langchain4j.dev/)
- [LangGraph Concepts](https://langchain-ai.github.io/langgraph/)
- [LangSmith Documentation](https://docs.smith.langchain.com/)
- [Agentic RAG Paper](https://arxiv.org/abs/2312.10997)
