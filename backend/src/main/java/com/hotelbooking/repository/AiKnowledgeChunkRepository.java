package com.hotelbooking.repository;

import com.hotelbooking.model.AiKnowledgeChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiKnowledgeChunkRepository extends JpaRepository<AiKnowledgeChunk, Long> {
    List<AiKnowledgeChunk> findByDocumentId(Long documentId);
    void deleteByDocumentId(Long documentId);

    @Query("SELECT c FROM AiKnowledgeChunk c WHERE " +
           "LOWER(c.content) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<AiKnowledgeChunk> searchByKeyword(@Param("keyword") String keyword);
}

