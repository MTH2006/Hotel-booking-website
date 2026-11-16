package com.hotelbooking.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "promotions")
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code; // Mã khuyến mãi [FR-51]

    @Column(columnDefinition = "TEXT")
    private String title; // Tiêu đề khuyến mãi

    @Column(name = "discount_percent", nullable = false)
    private Double discountPercent; // % giảm giá (ví dụ: 10, 20) [FR-53]

    @Column(name = "max_discount_amount")
    private Long maxDiscountAmount; // Số tiền giảm tối đa (VNĐ)

    @Column(name = "min_booking_amount")
    private Long minBookingAmount = 0L; // Giá trị đơn tối thiểu để áp dụng

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate; // Ngày bắt đầu [FR-52]

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate; // Ngày kết thúc [FR-52]

    @Column(name = "usage_limit")
    private Integer usageLimit = 100; // Giới hạn lượt sử dụng [FR-55]

    @Column(name = "used_count")
    private Integer usedCount = 0; // Số lượt đã sử dụng [FR-55]

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true; // Trạng thái hoạt động [FR-54]

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public Promotion() {
        this.createdAt = LocalDateTime.now();
        this.isActive = true;
        this.usedCount = 0;
    }

    public Promotion(String code, String title, Double discountPercent, Long maxDiscountAmount, Long minBookingAmount, LocalDate startDate, LocalDate endDate, Integer usageLimit) {
        this.code = code;
        this.title = title;
        this.discountPercent = discountPercent;
        this.maxDiscountAmount = maxDiscountAmount;
        this.minBookingAmount = minBookingAmount;
        this.startDate = startDate;
        this.endDate = endDate;
        this.usageLimit = usageLimit;
        this.usedCount = 0;
        this.isActive = true;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code != null ? code.trim().toUpperCase() : null; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Double getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(Double discountPercent) { this.discountPercent = discountPercent; }

    public Long getMaxDiscountAmount() { return maxDiscountAmount; }
    public void setMaxDiscountAmount(Long maxDiscountAmount) { this.maxDiscountAmount = maxDiscountAmount; }

    public Long getMinBookingAmount() { return minBookingAmount; }
    public void setMinBookingAmount(Long minBookingAmount) { this.minBookingAmount = minBookingAmount; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public Integer getUsageLimit() { return usageLimit; }
    public void setUsageLimit(Integer usageLimit) { this.usageLimit = usageLimit; }

    public Integer getUsedCount() { return usedCount; }
    public void setUsedCount(Integer usedCount) { this.usedCount = usedCount; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

