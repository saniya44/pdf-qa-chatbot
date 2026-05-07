package com.pdfchatbot.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles PDF parsing and text chunking.
 *
 * Flow:
 *   MultipartFile (PDF)
 *       → extractText()      — raw text string
 *       → splitIntoChunks()  — List<String> chunks (with overlap)
 */
@Slf4j
@Component
public class PdfProcessor {

    @Value("${app.rag.chunk-size:500}")
    private int chunkSize;

    @Value("${app.rag.chunk-overlap:50}")
    private int chunkOverlap;

    // ─────────────────────────────────────────────────────────
    //  EXTRACT TEXT FROM PDF
    // ─────────────────────────────────────────────────────────

    public String extractText(MultipartFile file) throws IOException {
        log.info("Extracting text from PDF: {}", file.getOriginalFilename());

        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            log.info("Extracted {} characters from {} pages",
                    text.length(), document.getNumberOfPages());

            return text;
        }
    }

    // ─────────────────────────────────────────────────────────
    //  SPLIT TEXT INTO OVERLAPPING CHUNKS
    // ─────────────────────────────────────────────────────────

    /**
     * Splits text into overlapping chunks for better RAG retrieval.
     *
     * Example with chunkSize=500, overlap=50:
     *   Chunk 1: chars [0   → 499]
     *   Chunk 2: chars [450 → 949]   ← 50 char overlap with chunk 1
     *   Chunk 3: chars [900 → 1399]
     *
     * Why overlap? Answers at chunk boundaries won't be missed.
     */
    public List<String> splitIntoChunks(String text) {
        List<String> chunks = new ArrayList<>();

        // Clean whitespace
        text = text.replaceAll("\\s+", " ").trim();

        int step = chunkSize - chunkOverlap;
        int start = 0;

        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            String chunk = text.substring(start, end).trim();

            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }

            start += step;
        }

        log.info("Split text into {} chunks (size={}, overlap={})",
                chunks.size(), chunkSize, chunkOverlap);

        return chunks;
    }

    // ─────────────────────────────────────────────────────────
    //  COMBINED: EXTRACT + SPLIT
    // ─────────────────────────────────────────────────────────

    public List<String> processFile(MultipartFile file) throws IOException {
        String text = extractText(file);
        return splitIntoChunks(text);
    }

    // ─────────────────────────────────────────────────────────
    //  VALIDATION
    // ─────────────────────────────────────────────────────────

    public void validatePdf(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty or null");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("application/pdf")) {
            throw new IllegalArgumentException(
                    "Invalid file type: " + contentType + ". Only PDF files are supported.");
        }
    }
}
