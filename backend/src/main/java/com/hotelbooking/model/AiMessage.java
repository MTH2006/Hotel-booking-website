package com.hotelbooking.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_messages")
public class AiMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Column(name = "sender_type", nullable = false, length = 20)
    private String senderType; // 'USER', 'ASSISTANT', 'SYSTEM'

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(length = 50)
    private String intent = "GENERAL_FAQ";

    @Column(columnDefinition = "TEXT")
    private String citations;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public AiMessage() {}

    public AiMessage(Long conversationId, String senderType, String content, String intent, String citations) {
        this.conversationId = conversationId;
        this.senderType = senderType;
        this.content = content;
        this.intent = intent != null ? intent : "GENERAL_FAQ";
        this.citations = citations;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }

    public String getSenderType() { return senderType; }
    public void setSenderType(String senderType) { this.senderType = senderType; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getIntent() { return intent; }
    public void setIntent(String intent) { this.intent = intent; }

    public String getCitations() { return citations; }
    public void setCitations(String citations) { this.citations = citations; }

    public String getMetadataJson() { return metadataJson; }
    public void setMetadataJson(String metadataJson) { this.metadataJson = metadataJson; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

