package com.hotelbooking.service;

import com.hotelbooking.model.AiKnowledgeChunk;
import com.hotelbooking.model.AiKnowledgeDocument;
import com.hotelbooking.repository.AiKnowledgeDocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RAGService {

    private static final Logger log = LoggerFactory.getLogger(RAGService.class);

    private final VectorStoreService vectorStoreService;
    private final AiKnowledgeDocumentRepository docRepo;

    public RAGService(VectorStoreService vectorStoreService, AiKnowledgeDocumentRepository docRepo) {
        this.vectorStoreService = vectorStoreService;
        this.docRepo = docRepo;
    }

    public static class RetrievalResult {
        private final boolean found;
        private final String combinedContext;
        private final List<String> citations;
        private final List<VectorStoreService.ScoredChunk> scoredChunks;

        public RetrievalResult(boolean found, String combinedContext, List<String> citations, List<VectorStoreService.ScoredChunk> scoredChunks) {
            this.found = found;
            this.combinedContext = combinedContext;
            this.citations = citations;
            this.scoredChunks = scoredChunks;
        }

        public boolean isFound() { return found; }
        public String getCombinedContext() { return combinedContext; }
        public List<String> getCitations() { return citations; }
        public List<VectorStoreService.ScoredChunk> getScoredChunks() { return scoredChunks; }
    }

    /**
     * Truy xuất các đoạn tri thức liên quan nhất cho câu hỏi của khách hàng.
     */
    public RetrievalResult retrieveContext(String query, int topK, double minThreshold) {
        List<VectorStoreService.ScoredChunk> matches = vectorStoreService.searchSimilar(query, topK, minThreshold);

        if (matches.isEmpty()) {
            return new RetrievalResult(false, "", Collections.emptyList(), Collections.emptyList());
        }

        StringBuilder contextBuilder = new StringBuilder();
        Set<String> citationsSet = new LinkedHashSet<>();

        for (int i = 0; i < matches.size(); i++) {
            AiKnowledgeChunk chunk = matches.get(i).getChunk();
            contextBuilder.append("[").append(i + 1).append("] ").append(chunk.getContent()).append("\n\n");

            docRepo.findById(chunk.getDocumentId()).ifPresent(doc -> {
                String cite = doc.getTitle() + " (" + doc.getCategory() + ")";
                citationsSet.add(cite);
            });
        }

        return new RetrievalResult(true, contextBuilder.toString().trim(), new ArrayList<>(citationsSet), matches);
    }
}

