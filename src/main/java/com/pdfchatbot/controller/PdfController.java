package com.pdfchatbot.controller;

import com.pdfchatbot.service.EmbeddingService;
import com.pdfchatbot.util.PdfProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * REST endpoints for PDF management.
 *
 *   POST   /api/pdf/upload     — Upload and index a PDF
 *   GET    /api/pdf/list       — List uploaded PDFs
 *   DELETE /api/pdf/clear      — Clear all indexed data
 */
@Slf4j
@RestController
@RequestMapping("/api/pdf")
@RequiredArgsConstructor
public class PdfController {

    private final PdfProcessor pdfProcessor;
    private final EmbeddingService embeddingService;

    // ─────────────────────────────────────────────────────────
    //  POST /api/pdf/upload
    // ─────────────────────────────────────────────────────────

    /**
     * Upload a PDF, extract text, chunk it, embed it, and store in vector DB.
     *
     * Test with curl:
     *   curl -X POST http://localhost:8080/api/pdf/upload \
     *        -F "file=@yourfile.pdf"
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> upload(
            @RequestParam("file") MultipartFile file) {

        try {
            log.info("Received PDF upload: {}", file.getOriginalFilename());

            // Validate
            pdfProcessor.validatePdf(file);

            // Extract text + split into chunks
            List<String> chunks = pdfProcessor.processFile(file);

            // Embed + store in vector DB
            int indexed = embeddingService.indexChunks(chunks, file.getOriginalFilename());

            return ResponseEntity.ok(Map.of(
                "success", true,
                "fileName", file.getOriginalFilename(),
                "totalChunks", indexed,
                "message", "PDF processed and indexed successfully!",
                "uploadedAt", LocalDateTime.now().toString()
            ));

        } catch (IllegalArgumentException e) {
            log.warn("Invalid file upload: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "Invalid File",
                "message", e.getMessage()
            ));

        } catch (Exception e) {
            log.error("Error processing PDF: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", "Processing Error",
                "message", "Failed to process PDF: " + e.getMessage()
            ));
        }
    }

    // ─────────────────────────────────────────────────────────
    //  GET /api/pdf/list
    // ─────────────────────────────────────────────────────────

    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> listFiles() {
        Map<String, Object> status = embeddingService.getStatus();
        return ResponseEntity.ok(status);
    }

    // ─────────────────────────────────────────────────────────
    //  DELETE /api/pdf/clear
    // ─────────────────────────────────────────────────────────

    @DeleteMapping("/clear")
    public ResponseEntity<Map<String, Object>> clearAll() {
        embeddingService.clearStore();
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "All indexed data has been cleared"
        ));
    }
}
