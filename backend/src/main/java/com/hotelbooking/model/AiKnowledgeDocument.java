package com.hotelbooking.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_knowledge_documents")
public class AiKnowledgeDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 50)
    private String category; // 'HOTEL_INFO', 'ROOM_INFO', 'POLICY', 'PAYMENT', 'FAQ'

    @Column(name = "source_type", length = 50)
    private String sourceType = "MANUAL"; // 'SYSTEM', 'DATABASE_SYNC', 'MANUAL'

    @Column(name = "hotel_id")
    private Long hotelId;

    @Column(length = 30)
    private String status = "INDEXED";

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public AiKnowledgeDocument() {}

    public AiKnowledgeDocument(String title, String category, String sourceType, Long hotelId) {
        this.title = title;
        this.category = category;
        this.sourceType = sourceType != null ? sourceType : "MANUAL";
        this.hotelId = hotelId;
        this.status = "INDEXED";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public Long getHotelId() { return hotelId; }
    public void setHotelId(Long hotelId) { this.hotelId = hotelId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

