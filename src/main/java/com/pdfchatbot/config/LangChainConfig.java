package com.pdfchatbot.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class LangChainConfig {

    @Value("${app.ai.provider}")
    private String aiProvider;

    @Value("${app.vectorstore.type}")
    private String vectorStoreType;

    // ── OpenAI ───────────────────────────────────────────────
    @Value("${app.openai.api-key}")
    private String openAiKey;

    @Value("${app.openai.chat-model}")
    private String openAiChatModel;

    @Value("${app.openai.embedding-model}")
    private String openAiEmbeddingModel;

    // ── Ollama ───────────────────────────────────────────────
    @Value("${app.ollama.base-url}")
    private String ollamaUrl;

    @Value("${app.ollama.chat-model}")
    private String ollamaChatModel;

    @Value("${app.ollama.embedding-model}")
    private String ollamaEmbeddingModel;

    // ── ChromaDB ─────────────────────────────────────────────
    @Value("${app.chroma.url}")
    private String chromaUrl;

    @Value("${app.chroma.collection-name}")
    private String chromaCollection;

    // ─────────────────────────────────────────────────────────
    //  EMBEDDING MODEL
    // ─────────────────────────────────────────────────────────

    @Bean
    public EmbeddingModel embeddingModel() {
        return switch (aiProvider.toLowerCase()) {

            case "openai" -> {
                log.info("Using OpenAI embedding model: {}", openAiEmbeddingModel);
                yield OpenAiEmbeddingModel.builder()
                        .apiKey(openAiKey)
                        .modelName(openAiEmbeddingModel)
                        .build();
            }

            case "ollama" -> {
                log.info("Using Ollama embedding model: {}", ollamaEmbeddingModel);
                yield OllamaEmbeddingModel.builder()
                        .baseUrl(ollamaUrl)
                        .modelName(ollamaEmbeddingModel)
                        .build();
            }

            // FREE: local ONNX model — no API key needed, runs in JVM
            default -> {
                log.info("Using built-in AllMiniLmL6V2 embedding model (no API key needed)");
                yield new AllMiniLmL6V2EmbeddingModel();
            }
        };
    }

    // ─────────────────────────────────────────────────────────
    //  CHAT MODEL
    // ─────────────────────────────────────────────────────────

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return switch (aiProvider.toLowerCase()) {

            case "openai" -> {
                log.info("Using OpenAI chat model: {}", openAiChatModel);
                yield OpenAiChatModel.builder()
                        .apiKey(openAiKey)
                        .modelName(openAiChatModel)
                        .temperature(0.3)   // Lower = more factual answers
                        .build();
            }

            case "ollama" -> {
                log.info("Using Ollama chat model: {}", ollamaChatModel);
                yield OllamaChatModel.builder()
                        .baseUrl(ollamaUrl)
                        .modelName(ollamaChatModel)
                        .temperature(0.3)
                        .build();
            }

            default -> throw new IllegalArgumentException(
                    "Unknown AI provider: " + aiProvider + ". Use 'openai' or 'ollama'");
        };
    }

    // ─────────────────────────────────────────────────────────
    //  VECTOR STORE (Embedding Store)
    // ─────────────────────────────────────────────────────────

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        return switch (vectorStoreType.toLowerCase()) {

            case "chroma" -> {
                log.info("Using ChromaDB at {} (collection: {})", chromaUrl, chromaCollection);
                yield ChromaEmbeddingStore.builder()
                        .baseUrl(chromaUrl)
                        .collectionName(chromaCollection)
                        .build();
            }

            // In-memory: resets on app restart — great for development
            default -> {
                log.info("Using In-Memory vector store (data resets on restart)");
                yield new InMemoryEmbeddingStore<>();
            }
        };
    }
}
