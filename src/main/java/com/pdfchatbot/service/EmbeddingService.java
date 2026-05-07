package com.pdfchatbot.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * EmbeddingService handles the RAG pipeline:
 *
 *   INGEST:  text chunks → embeddings → vector store
 *   QUERY:   question    → embedding  → similarity search → top-K chunks
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    @Value("${app.rag.max-results:5}")
    private int maxResults;

    // Track uploaded files (in-memory, resets on restart)
    private final Map<String, Integer> uploadedFiles = new ConcurrentHashMap<>();
    private final AtomicInteger totalChunks = new AtomicInteger(0);

    // ─────────────────────────────────────────────────────────
    //  INGEST: Embed chunks and store in vector DB
    // ─────────────────────────────────────────────────────────

    /**
     * Takes raw text chunks from a PDF and stores their embeddings.
     *
     * @param chunks   List of text chunks from the PDF
     * @param fileName Name of the source PDF file
     * @return Number of chunks indexed
     */
    public int indexChunks(List<String> chunks, String fileName) {
        log.info("Indexing {} chunks from '{}'", chunks.size(), fileName);

        int indexed = 0;

        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);

            // Add metadata: source file + chunk index
            TextSegment segment = TextSegment.from(
                    chunk,
                    dev.langchain4j.data.document.Metadata.from(
                            Map.of(
                                "source", fileName,
                                "chunkIndex", String.valueOf(i)
                            )
                    )
            );

            // Create embedding vector for this chunk
            Embedding embedding = embeddingModel.embed(segment).content();

            // Store (embedding vector, text segment) pair in vector DB
            embeddingStore.add(embedding, segment);
            indexed++;
        }

        uploadedFiles.put(fileName, chunks.size());
        totalChunks.addAndGet(indexed);

        log.info("Successfully indexed {} chunks from '{}'", indexed, fileName);
        return indexed;
    }

    // ─────────────────────────────────────────────────────────
    //  RETRIEVE: Similarity search for a query
    // ─────────────────────────────────────────────────────────

    /**
     * Finds the most relevant text chunks for a given question.
     *
     * @param question User's question
     * @return Top-K most similar text chunks
     */
    public List<String> findRelevantChunks(String question) {
        log.info("Searching for relevant chunks for question: '{}'", question);

        // Embed the user's question
        Embedding questionEmbedding = embeddingModel.embed(question).content();

        // Similarity search in vector store
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(questionEmbedding)
                .maxResults(maxResults)
                .minScore(0.5)   // Only return chunks with >50% similarity
                .build();

        List<EmbeddingMatch<TextSegment>> matches =
                embeddingStore.search(request).matches();

        List<String> chunks = matches.stream()
                .map(match -> match.embedded().text())
                .toList();

        log.info("Found {} relevant chunks (score threshold: 0.5)", chunks.size());
        return chunks;
    }

    // ─────────────────────────────────────────────────────────
    //  STATUS
    // ─────────────────────────────────────────────────────────

    public Map<String, Object> getStatus() {
        return Map.of(
            "uploadedFiles", uploadedFiles,
            "totalChunksIndexed", totalChunks.get(),
            "totalDocuments", uploadedFiles.size()
        );
    }

    public void clearStore() {
        // Note: InMemoryEmbeddingStore doesn't have a clear() method
        // For ChromaDB, the collection must be deleted/recreated
        uploadedFiles.clear();
        totalChunks.set(0);
        log.warn("File tracking cleared. Note: vector store data may persist depending on implementation.");
    }
}
