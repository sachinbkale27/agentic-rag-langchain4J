# Agentic RAG - Java Spring Boot + LangChain4j

This is a Java Spring Boot implementation of an Agentic RAG (Retrieval-Augmented Generation) system using LangChain4j. It's a conversion of the original Python project that uses LangChain and LangGraph.

## Features

- **Question Routing**: Automatically routes questions to either vectorstore or web search
- **Document Retrieval**: Retrieves relevant documents from Chroma vector database
- **Document Grading**: Evaluates relevance of retrieved documents
- **Answer Generation**: Generates answers using OpenAI GPT-4
- **Hallucination Detection**: Validates that answers are grounded in facts
- **Answer Quality Check**: Ensures answers address the question
- **Web Search Fallback**: Uses Tavily API for web search when needed
- **LangSmith Integration**: Full observability and tracing support (optional)

## Architecture

### Components

1. **Model Classes**: Data structures for graph state and grading results
2. **Chain Services**: LLM-based operations (routing, grading, generation)
3. **Node Services**: Graph workflow operations
4. **Graph Workflow**: Orchestrates the agentic RAG flow
5. **Supporting Services**: Vector store, web search, and document ingestion

### Workflow

```
Question → Route → [Vectorstore OR Web Search]
                          ↓
                    Grade Documents
                          ↓
                   [Generate OR Web Search]
                          ↓
                     Generate Answer
                          ↓
                  Check Hallucinations
                          ↓
                   Check Answer Quality
                          ↓
                    Final Answer
```

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- OpenAI API key
- Tavily API key
- Chroma vector database (running locally or remotely)
- LangSmith API key (optional, for tracing and monitoring)

## Setup

1. **Clone and navigate to the project**:
   ```bash
   cd /Users/sachinkale/projects/java/agentic-rag
   ```

2. **Set up environment variables**:
   ```bash
   cp .env.example .env
   # Edit .env and add your API keys
   ```

3. **Start Chroma (if not already running)**:
   ```bash
   docker run -p 8000:8000 chromadb/chroma
   ```

4. **Build the project**:
   ```bash
   mvn clean install
   ```

5. **Run document ingestion** (first time only):
   - Uncomment the `ingestDocuments()` call in `AgenticRagApplication.java`
   - Run the application
   - Comment it back after ingestion completes

6. **Run the application**:
   ```bash
   mvn spring-boot:run
   ```

## Configuration

Edit `src/main/resources/application.properties` to configure:

- OpenAI model settings
- Chroma connection details
- LangSmith tracing (optional)
- Logging levels
- Collection names

### LangSmith Tracing (Optional)

To enable LangSmith tracing for monitoring and debugging:

```bash
export LANGSMITH_TRACING_V2=true
export LANGSMITH_API_KEY=your_langsmith_api_key
export LANGSMITH_PROJECT=agentic-rag-java
```

See [LANGSMITH_GUIDE.md](LANGSMITH_GUIDE.md) for detailed instructions.

## Usage

The main application demonstrates the workflow with a sample question. You can modify the question in `AgenticRagApplication.java`:

```java
String question = "How to make pizza?";
GraphState result = workflow.invoke(question);
```

## Project Structure

```
src/main/java/com/example/agenticrag/
├── chain/              # LLM chain services
│   ├── AnswerGrader.java
│   ├── GenerationChain.java
│   ├── HallucinationGrader.java
│   ├── QuestionRouter.java
│   └── RetrievalGrader.java
├── graph/              # Workflow orchestration
│   └── AgenticRagWorkflow.java
├── model/              # Data models
│   ├── GradeAnswer.java
│   ├── GradeDocuments.java
│   ├── GradeHallucinations.java
│   ├── GraphConstants.java
│   ├── GraphState.java
│   └── RouteQuery.java
├── node/               # Workflow nodes
│   ├── GenerateNode.java
│   ├── GradeDocumentsNode.java
│   ├── RetrieveNode.java
│   └── WebSearchNode.java
├── config/             # Configuration
│   └── LangSmithConfig.java
├── service/            # Supporting services
│   ├── IngestionService.java
│   ├── LangSmithTracingService.java
│   ├── TavilySearchService.java
│   └── VectorStoreService.java
└── AgenticRagApplication.java
```

## Differences from Python Version

1. **Type Safety**: Java's strong typing vs Python's dynamic typing
2. **Dependency Injection**: Spring's IoC container vs Python's manual wiring
3. **LangChain4j**: Java implementation of LangChain concepts
4. **No LangGraph**: Custom workflow orchestration instead of LangGraph
5. **Configuration**: application.properties vs .env files

## Testing

Run tests with:
```bash
mvn test
```

## License

Same as the original Python project.

## Contributing

Contributions are welcome! Please follow standard Java coding conventions.
