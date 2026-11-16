package com.hotelbooking.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", unique = true, nullable = false, length = 64)
    private String transactionId; // Mã giao dịch thanh toán [FR-42]

    @Column(name = "booking_id", nullable = false, length = 50)
    private String bookingId; // Mã đơn đặt phòng [FR-39]

    @Column(nullable = false)
    private Long amount; // Số tiền thanh toán [FR-39]

    @Column(name = "payment_method", nullable = false, length = 50)
    private String paymentMethod; // VNPAY, CREDIT_CARD, BANK_TRANSFER, CASH_ON_ARRIVAL [FR-38]

    @Column(nullable = false, length = 30)
    private String status = "SUCCESS"; // PENDING, SUCCESS, FAILED, REFUNDED [FR-41]

    @Column(name = "payment_gateway", length = 50)
    private String paymentGateway = "MOCK_GATEWAY"; // Cổng thanh toán giả lập [FR-40]

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt; // [FR-43]

    public Payment() {
        this.createdAt = LocalDateTime.now();
        this.status = "SUCCESS";
    }

    public Payment(String transactionId, String bookingId, Long amount, String paymentMethod, String status, String note) {
        this.transactionId = transactionId;
        this.bookingId = bookingId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.note = note;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public Long getAmount() { return amount; }
    public void setAmount(Long amount) { this.amount = amount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPaymentGateway() { return paymentGateway; }
    public void setPaymentGateway(String paymentGateway) { this.paymentGateway = paymentGateway; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getRefundedAt() { return refundedAt; }
    public void setRefundedAt(LocalDateTime refundedAt) { this.refundedAt = refundedAt; }
}

