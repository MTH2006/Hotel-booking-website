package com.hotelbooking.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);
    private static final int VECTOR_DIM = 128;
    private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private static final Pattern WORD_PATTERN = Pattern.compile("[\\p{L}\\p{Nd}]+");

    /**
     * Sinh vector đặc trưng ngữ nghĩa dense 128 chiều từ đoạn văn bản tiếng Việt / tiếng Anh.
     */
    public float[] getEmbedding(String text) {
        float[] vector = new float[VECTOR_DIM];
        if (text == null || text.isBlank()) {
            return vector;
        }

        String normalized = text.toLowerCase(Locale.ROOT).trim();
        List<String> tokens = extractTokens(normalized);

        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            int hash1 = Math.abs(token.hashCode());
            int idx1 = hash1 % VECTOR_DIM;
            vector[idx1] += 1.0f;

            // Character tri-grams for subword matching (e.g. "khách", "phòng", "hủy")
            if (token.length() >= 3) {
                for (int c = 0; c <= token.length() - 3; c++) {
                    String trigram = token.substring(c, c + 3);
                    int hashTri = Math.abs(trigram.hashCode() * 31);
                    int idxTri = hashTri % VECTOR_DIM;
                    vector[idxTri] += 0.35f;
                }
            }

            // Word bi-grams for phrase semantics (e.g. "đặt phòng", "hủy phòng", "giá bao nhiêu", "nhận phòng")
            if (i < tokens.size() - 1) {
                String bigram = token + "_" + tokens.get(i + 1);
                int hashBi = Math.abs(bigram.hashCode() * 17);
                int idxBi = hashBi % VECTOR_DIM;
                vector[idxBi] += 0.8f;
            }
        }

        // L2 Normalize
        normalize(vector);
        return vector;
    }

    /**
     * Tính Cosine Similarity giữa 2 vector.
     */
    public double cosineSimilarity(float[] vecA, float[] vecB) {
        if (vecA == null || vecB == null || vecA.length != vecB.length) {
            return 0.0;
        }
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vecA.length; i++) {
            dotProduct += vecA[i] * vecB[i];
            normA += vecA[i] * vecA[i];
            normB += vecB[i] * vecB[i];
        }

        if (normA <= 0.0 || normB <= 0.0) {
            return 0.0;
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private void normalize(float[] vector) {
        double sumSq = 0.0;
        for (float v : vector) {
            sumSq += v * v;
        }
        if (sumSq > 0.0) {
            float norm = (float) Math.sqrt(sumSq);
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= norm;
            }
        }
    }

    private List<String> extractTokens(String text) {
        List<String> tokens = new ArrayList<>();
        var matcher = WORD_PATTERN.matcher(text);
        while (matcher.find()) {
            tokens.add(matcher.group());
        }
        return tokens;
    }

    public static String stripAccents(String s) {
        if (s == null) return "";
        String normalized = Normalizer.normalize(s, Normalizer.Form.NFD);
        return DIACRITICS_PATTERN.matcher(normalized).replaceAll("").replace('đ', 'd').replace('Đ', 'D');
    }
}

