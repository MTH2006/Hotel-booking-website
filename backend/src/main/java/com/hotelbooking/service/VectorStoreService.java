package com.hotelbooking.service;

import com.hotelbooking.model.AiKnowledgeChunk;
import com.hotelbooking.repository.AiKnowledgeChunkRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VectorStoreService {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreService.class);

    private final AiKnowledgeChunkRepository chunkRepository;
    private final EmbeddingService embeddingService;

    // Bộ nhớ đệm vector trong bộ nhớ để truy vấn cực nhanh (<1ms)
    private final Map<Long, float[]> vectorCache = new ConcurrentHashMap<>();

    public VectorStoreService(AiKnowledgeChunkRepository chunkRepository, EmbeddingService embeddingService) {
        this.chunkRepository = chunkRepository;
        this.embeddingService = embeddingService;
    }

    @PostConstruct
    public void initCache() {
        refreshIndex();
    }

    public synchronized void refreshIndex() {
        vectorCache.clear();
        List<AiKnowledgeChunk> chunks = chunkRepository.findAll();
        for (AiKnowledgeChunk chunk : chunks) {
            float[] vec = embeddingService.getEmbedding(chunk.getContent());
            vectorCache.put(chunk.getId(), vec);
        }
        log.info("VectorStoreService indexed {} knowledge chunks in memory.", vectorCache.size());
    }

    public static class ScoredChunk {
        private final AiKnowledgeChunk chunk;
        private final double score;

        public ScoredChunk(AiKnowledgeChunk chunk, double score) {
            this.chunk = chunk;
            this.score = score;
        }

        public AiKnowledgeChunk getChunk() { return chunk; }
        public double getScore() { return score; }
    }

    /**
     * Tìm kiếm Hybrid (Vector Cosine + Lexical Keyword) top-K chunks tương đồng nhất.
     */
    public List<ScoredChunk> searchSimilar(String query, int topK, double minThreshold) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        float[] queryVec = embeddingService.getEmbedding(query);
        String strippedQuery = EmbeddingService.stripAccents(query.toLowerCase());
        String[] keywords = strippedQuery.split("\\s+");

        List<AiKnowledgeChunk> allChunks = chunkRepository.findAll();
        List<ScoredChunk> scoredList = new ArrayList<>();

        for (AiKnowledgeChunk chunk : allChunks) {
            float[] chunkVec = vectorCache.computeIfAbsent(chunk.getId(), id -> embeddingService.getEmbedding(chunk.getContent()));
            double cosine = embeddingService.cosineSimilarity(queryVec, chunkVec);

            // Điểm thưởng lexical keyword matching
            String strippedContent = EmbeddingService.stripAccents(chunk.getContent().toLowerCase());
            double lexicalBoost = 0.0;
            int matchedKeywords = 0;
            for (String kw : keywords) {
                if (kw.length() >= 3 && strippedContent.contains(kw)) {
                    lexicalBoost += 0.08;
                    matchedKeywords++;
                }
            }

            double totalScore = (cosine * 0.75) + Math.min(0.25, lexicalBoost);

            boolean isRelevant = (matchedKeywords > 0 && totalScore >= minThreshold) || (cosine >= 0.65);

            if (isRelevant) {
                scoredList.add(new ScoredChunk(chunk, totalScore));
            }
        }

        scoredList.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        return scoredList.stream().limit(topK).toList();
    }
}

