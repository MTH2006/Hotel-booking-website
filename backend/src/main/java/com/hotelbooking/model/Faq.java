
package com.hotelbooking.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "faqs")
public class Faq {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FR-96: Cau hoi thuong gap
    @Column(nullable = false, length = 500)
    private String question;

    // FR-96: Noi dung tra loi
    @Column(nullable = false, columnDefinition = "TEXT")
    private String answer;

    // Thoi gian tao FAQ
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Thoi gian cap nhat FAQ
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Faq() {
    }

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
