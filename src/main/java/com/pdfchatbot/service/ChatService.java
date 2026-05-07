package com.pdfchatbot.service;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * ChatService handles the Q&A flow:
 *
 *   1. Retrieve relevant chunks from EmbeddingService
 *   2. Build a prompt: System + Context + User question
 *   3. Send to LLM and return the answer
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatLanguageModel chatLanguageModel;
    private final EmbeddingService embeddingService;

    // ─────────────────────────────────────────────────────────
    //  SYSTEM PROMPT
    // ─────────────────────────────────────────────────────────

    private static final String SYSTEM_PROMPT = """
            You are an intelligent assistant specialized in answering questions
            about PDF documents.

            INSTRUCTIONS:
            - Answer ONLY based on the context provided below.
            - If the context does not contain enough information to answer,
              say: "I couldn't find relevant information in the uploaded PDF."
            - Keep answers concise, accurate, and well-structured.
            - Do not make up information that is not in the context.
            - If asked for a list, use bullet points.
            """;

    // ─────────────────────────────────────────────────────────
    //  MAIN Q&A METHOD
    // ─────────────────────────────────────────────────────────

    /**
     * Answers a question using RAG:
     *   Retrieve → Augment prompt → Generate answer
     *
     * @param question The user's question
     * @return ChatResult with answer and source chunks
     */
    public ChatResult ask(String question) {
        long start = System.currentTimeMillis();
        log.info("Processing question: '{}'", question);

        // Step 1: Retrieve relevant context from vector store
        List<String> relevantChunks = embeddingService.findRelevantChunks(question);

        if (relevantChunks.isEmpty()) {
            return ChatResult.noContext(question,
                    "No relevant content found in the uploaded PDFs. " +
                    "Please upload a PDF first, or try rephrasing your question.");
        }

        // Step 2: Build context string from retrieved chunks
        String context = buildContext(relevantChunks);

        // Step 3: Build the full prompt
        String userPrompt = buildUserPrompt(question, context);

        // Step 4: Call LLM
        Response<AiMessage> response = chatLanguageModel.generate(
                SystemMessage.from(SYSTEM_PROMPT),
                UserMessage.from(userPrompt)
        );

        String answer = response.content().text();
        long elapsed = System.currentTimeMillis() - start;

        log.info("Answer generated in {}ms", elapsed);

        return ChatResult.builder()
                .question(question)
                .answer(answer)
                .sourceChunks(relevantChunks)
                .processingTimeMs(elapsed)
                .build();
    }

    // ─────────────────────────────────────────────────────────
    //  PROMPT BUILDING
    // ─────────────────────────────────────────────────────────

    private String buildContext(List<String> chunks) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < chunks.size(); i++) {
            sb.append("--- Context Chunk ").append(i + 1).append(" ---\n");
            sb.append(chunks.get(i)).append("\n\n");
        }

        return sb.toString();
    }

    private String buildUserPrompt(String question, String context) {
        return """
                CONTEXT FROM PDF:
                %s
                
                QUESTION: %s
                
                Please answer the question based only on the context above.
                """.formatted(context, question);
    }

    // ─────────────────────────────────────────────────────────
    //  RESULT RECORD
    // ─────────────────────────────────────────────────────────

    @lombok.Builder
    @lombok.Data
    public static class ChatResult {
        private String question;
        private String answer;
        private List<String> sourceChunks;
        private long processingTimeMs;

        public static ChatResult noContext(String question, String message) {
            return ChatResult.builder()
                    .question(question)
                    .answer(message)
                    .sourceChunks(List.of())
                    .processingTimeMs(0)
                    .build();
        }
    }
}
