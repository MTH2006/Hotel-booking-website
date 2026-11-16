package com.hotelbooking.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_handoff_requests")
public class AiHandoffRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id")
    private Long conversationId;

    @Column(name = "customer_name", length = 150)
    private String customerName;

    @Column(name = "customer_email", length = 255)
    private String customerEmail;

    @Column(name = "customer_phone", length = 20)
    private String customerPhone;

    @Column(name = "question_summary", columnDefinition = "TEXT", nullable = false)
    private String questionSummary;

    @Column(length = 30)
    private String status = "PENDING"; // 'PENDING', 'RESOLVED'

    @Column(name = "response_note", columnDefinition = "TEXT")
    private String responseNote;

    @Column(name = "assigned_staff_id")
    private Long assignedStaffId;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "staff_reply", columnDefinition = "TEXT")
    private String staffReply;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    public AiHandoffRequest() {}

    public AiHandoffRequest(Long conversationId, String customerName, String customerEmail, String customerPhone, String questionSummary) {
        this.conversationId = conversationId;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.customerPhone = customerPhone;
        this.questionSummary = questionSummary;
        this.status = "PENDING";
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public String getQuestionSummary() { return questionSummary; }
    public void setQuestionSummary(String questionSummary) { this.questionSummary = questionSummary; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getResponseNote() { return responseNote; }
    public void setResponseNote(String responseNote) { this.responseNote = responseNote; }

    public Long getAssignedStaffId() { return assignedStaffId; }
    public void setAssignedStaffId(Long assignedStaffId) { this.assignedStaffId = assignedStaffId; }

    public LocalDateTime getAssignedAt() { return assignedAt; }
    public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }

    public String getStaffReply() { return staffReply; }
    public void setStaffReply(String staffReply) { this.staffReply = staffReply; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
}

