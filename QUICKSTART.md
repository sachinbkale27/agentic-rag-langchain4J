# Quick Start Guide

## 1. Setup Environment Variables

```bash
cd /Users/sachinkale/projects/java/agentic-rag
cp .env.example .env
```

Edit `.env` and add your API keys:
```
OPENAI_API_KEY=sk-...
TAVILY_API_KEY=tvly-...
```

Export them:
```bash
export $(cat .env | xargs)
```

## 2. Start Chroma Database

```bash
make docker-chroma
# Or manually:
# docker run -d -p 8000:8000 --name chroma chromadb/chroma
```

## 3. Ingest Documents (First Time Only)

Edit `AgenticRagApplication.java` and uncomment this line:
```java
// ingestDocuments();  // <- Remove the //
```

Then run:
```bash
make run
```

After ingestion completes, comment it back and restart.

## 4. Run the Application

```bash
make run
```

## 5. Test with Your Own Questions

Edit the question in `AgenticRagApplication.java`:
```java
String question = "What are the different types of agents?";
```

## Common Commands

```bash
make build          # Build the project
make run           # Run the application
make test          # Run tests
make clean         # Clean build artifacts
make docker-chroma # Start Chroma in Docker
```

## Troubleshooting

### Chroma Connection Error
- Ensure Chroma is running: `docker ps | grep chroma`
- Check the URL in `application.properties`

### API Key Issues
- Verify environment variables are exported: `echo $OPENAI_API_KEY`
- Check Spring can read them in logs

### Build Issues
- Ensure Java 17+ is installed: `java -version`
- Update Maven wrapper: `./mvnw --version`
