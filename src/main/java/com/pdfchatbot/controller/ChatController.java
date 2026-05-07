package com.pdfchatbot.controller;

import com.pdfchatbot.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * REST endpoints for Q&A chat.
 *
 *   POST /api/chat/ask     — Ask a question
 *   GET  /api/chat/health  — Health check
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    // ─────────────────────────────────────────────────────────
    //  POST /api/chat/ask
    // ─────────────────────────────────────────────────────────

    /**
     * Ask a question about the uploaded PDFs.
     *
     * Request body:
     *   {
     *     "question": "What is this document about?"
     *   }
     *
     * Test with curl:
     *   curl -X POST http://localhost:8080/api/chat/ask \
     *        -H "Content-Type: application/json" \
     *        -d '{"question": "What is this document about?"}'
     */
    @PostMapping("/ask")
    public ResponseEntity<Map<String, Object>> ask(
            @RequestBody Map<String, String> request) {

        String question = request.get("question");

        if (question == null || question.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "Question cannot be empty"
            ));
        }

        try {
            log.info("Chat request: '{}'", question);
            ChatService.ChatResult result = chatService.ask(question.trim());

            return ResponseEntity.ok(Map.of(
                "success", true,
                "question", result.getQuestion(),
                "answer", result.getAnswer(),
                "sourceChunks", result.getSourceChunks(),
                "processingTimeMs", result.getProcessingTimeMs(),
                "timestamp", LocalDateTime.now().toString()
            ));

        } catch (Exception e) {
            log.error("Error processing question: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", "Failed to process question: " + e.getMessage()
            ));
        }
    }

    // ─────────────────────────────────────────────────────────
    //  GET /api/chat/health
    // ─────────────────────────────────────────────────────────

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "PDF Q&A Chatbot",
            "timestamp", LocalDateTime.now().toString()
        ));
    }
}
