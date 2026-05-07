# 📄 PDF Q&A Chatbot — Spring Boot + LangChain4j + RAG

> Intelligent document Q&A using Retrieval-Augmented Generation (RAG).
> Upload any PDF → Ask questions → Get context-aware answers.

---

## 🧠 Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         RAG PIPELINE                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   INGEST FLOW                                                   │
│   ──────────                                                    │
│   PDF File → Extract Text → Split into Chunks → Embed Chunks   │
│                                              → Store in VectorDB│
│                                                                 │
│   QUERY FLOW                                                    │
│   ──────────                                                    │
│   User Question → Embed Question → Similarity Search           │
│                                 → Top-K Chunks Retrieved       │
│                                 → Build Prompt (Context+Q)     │
│                                 → LLM Generates Answer         │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🔧 Tech Stack

| Layer          | Technology                          |
|---------------|-------------------------------------|
| Backend        | Spring Boot 3.2 (Java 17)           |
| AI Framework   | LangChain4j 0.35                    |
| LLM            | OpenAI GPT-3.5 / Ollama (local)     |
| Embeddings     | OpenAI Ada / AllMiniLM (local ONNX) |
| Vector Store   | ChromaDB / In-Memory                |
| PDF Parsing    | Apache PDFBox 3.x                   |
| Frontend       | React + Dropzone                    |

---

## 🚀 Quick Start

### Option 1 — OpenAI (Paid, easiest)

1. **Clone and configure**
   ```bash
   git clone <your-repo>
   cd pdf-chatbot
   ```

2. **Set your API key** in `src/main/resources/application.properties`:
   ```properties
   app.ai.provider=openai
   app.openai.api-key=sk-YOUR_KEY_HERE
   ```

3. **Run the backend**
   ```bash
   mvn spring-boot:run
   ```

4. **Test it**
   ```bash
   # Upload a PDF
   curl -X POST http://localhost:8080/api/pdf/upload \
        -F "file=@sample.pdf"

   # Ask a question
   curl -X POST http://localhost:8080/api/chat/ask \
        -H "Content-Type: application/json" \
        -d '{"question": "What is this document about?"}'
   ```

---

### Option 2 — Ollama (Free, fully local)

1. **Start infrastructure**
   ```bash
   docker-compose up -d
   ```

2. **Pull Ollama models**
   ```bash
   docker exec pdf-chatbot-ollama ollama pull llama3
   docker exec pdf-chatbot-ollama ollama pull nomic-embed-text
   ```

3. **Configure for Ollama** in `application.properties`:
   ```properties
   app.ai.provider=ollama
   app.vectorstore.type=chroma
   ```

4. **Run the backend**
   ```bash
   mvn spring-boot:run
   ```

---

## 📡 API Endpoints

### PDF Management

| Method | Endpoint          | Description                    |
|--------|------------------|--------------------------------|
| POST   | `/api/pdf/upload` | Upload & index a PDF           |
| GET    | `/api/pdf/list`   | List uploaded PDFs + status    |
| DELETE | `/api/pdf/clear`  | Clear all indexed data         |

**Upload example:**
```bash
curl -X POST http://localhost:8080/api/pdf/upload \
     -F "file=@your-document.pdf"
```

**Response:**
```json
{
   "success": true,
   "fileName": "your-document.pdf",
   "totalChunks": 47,
   "message": "PDF processed and indexed successfully!",
   "uploadedAt": "2025-05-05T10:30:00"
}
```

### Chat / Q&A

| Method | Endpoint         | Description         |
|--------|-----------------|---------------------|
| POST   | `/api/chat/ask`  | Ask a question      |
| GET    | `/api/chat/health` | Health check      |

**Ask example:**
```bash
curl -X POST http://localhost:8080/api/chat/ask \
     -H "Content-Type: application/json" \
     -d '{"question": "What are the key findings in this report?"}'
```

**Response:**
```json
{
   "success": true,
   "question": "What are the key findings in this report?",
   "answer": "Based on the document, the key findings are...",
   "sourceChunks": ["chunk1 text...", "chunk2 text..."],
   "processingTimeMs": 1340,
   "timestamp": "2025-05-05T10:31:00"
}
```

---

## 📁 Project Structure

```
pdf-chatbot/
├── src/main/java/com/pdfchatbot/
│   ├── PdfChatbotApplication.java        # Main entry point
│   ├── config/
│   │   ├── LangChainConfig.java          # LLM + Vector store wiring
│   │   └── CorsConfig.java               # CORS for React frontend
│   ├── controller/
│   │   ├── PdfController.java            # PDF upload/manage endpoints
│   │   └── ChatController.java           # Q&A endpoints
│   ├── service/
│   │   ├── EmbeddingService.java         # Embed + store + retrieve chunks
│   │   └── ChatService.java              # RAG: retrieve + prompt + generate
│   └── util/
│       └── PdfProcessor.java             # PDF text extraction + chunking
├── src/main/resources/
│   └── application.properties            # All configuration
├── frontend/src/
│   └── App.jsx                           # React Dropzone + Chat UI
├── docker-compose.yml                    # ChromaDB + Ollama
└── pom.xml                               # Maven dependencies
```

---

> Built an intelligent PDF Q&A chatbot using Spring Boot and LangChain4j,
> implementing RAG architecture with document chunking, embedding-based
> semantic retrieval using ChromaDB, and context-aware LLM responses via OpenAI/Ollama.

---

## 🆙 Upgrade Path

| Feature               | How to Add                              |
|----------------------|-----------------------------------------|
| Conversation memory   | `MessageWindowChatMemory` in LangChain4j |
| Multiple PDF sessions | Add session ID + filter by metadata     |
| Authentication        | Spring Security + JWT                  |
| Streaming responses   | `StreamingChatLanguageModel`            |
| Better chunking       | `RecursiveCharacterTextSplitter`        |

<<<<<<< HEAD
---
=======
---
>>>>>>> b7c1fcbee86c4882f3e205603ff28b58c20e49e6
