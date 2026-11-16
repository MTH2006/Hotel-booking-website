package com.hotelbooking.repository;

import com.hotelbooking.model.AiKnowledgeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiKnowledgeDocumentRepository extends JpaRepository<AiKnowledgeDocument, Long> {
    List<AiKnowledgeDocument> findByCategory(String category);
    List<AiKnowledgeDocument> findByHotelId(Long hotelId);
    List<AiKnowledgeDocument> findAllByOrderByCreatedAtDesc();
}

