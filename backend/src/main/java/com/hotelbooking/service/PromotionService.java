package com.hotelbooking.service;

import com.hotelbooking.model.Promotion;
import com.hotelbooking.repository.PromotionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PromotionService {

    private final PromotionRepository promotionRepository;

    public PromotionService(PromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

    // FR-51: Xem danh sách khuyến mãi đang hoạt động
    public List<Promotion> getActivePromotions() {
        return promotionRepository.findByIsActiveTrueOrderByCreatedAtDesc();
    }

    // Quản trị: Xem tất cả khuyến mãi
    public List<Promotion> getAllPromotions() {
        return promotionRepository.findAllByOrderByCreatedAtDesc();
    }

    public Optional<Promotion> getPromotionById(Long id) {
        return promotionRepository.findById(id);
    }

    // FR-52 & FR-53: Kiểm tra và tính giảm giá
    public Map<String, Object> applyPromotion(String code, Long bookingAmount) {
        Map<String, Object> result = new HashMap<>();

        if (code == null || code.isBlank()) {
            result.put("valid", false);
            result.put("message", "Vui lòng nhập mã khuyến mãi.");
            return result;
        }

        Optional<Promotion> opt = promotionRepository.findByCodeIgnoreCase(code.trim());
        if (opt.isEmpty()) {
            result.put("valid", false);
            result.put("message", "Mã khuyến mãi không tồn tại.");
            return result;
        }

        Promotion promo = opt.get();

        if (Boolean.FALSE.equals(promo.getIsActive())) {
            result.put("valid", false);
            result.put("message", "Mã khuyến mãi này đã tạm ngưng áp dụng.");
            return result;
        }

        LocalDate today = LocalDate.now();
        if (promo.getStartDate() != null && today.isBefore(promo.getStartDate())) {
            result.put("valid", false);
            result.put("message", "Chương trình khuyến mãi chưa bắt đầu (Từ " + promo.getStartDate() + ").");
            return result;
        }

        if (promo.getEndDate() != null && today.isAfter(promo.getEndDate())) {
            result.put("valid", false);
            result.put("message", "Mã khuyến mãi đã hết hạn sử dụng (Hết hạn: " + promo.getEndDate() + ").");
            return result;
        }

        if (promo.getUsageLimit() != null && promo.getUsedCount() != null && promo.getUsedCount() >= promo.getUsageLimit()) {
            result.put("valid", false);
            result.put("message", "Mã khuyến mãi đã hết lượt sử dụng.");
            return result;
        }

        if (promo.getMinBookingAmount() != null && bookingAmount != null && bookingAmount < promo.getMinBookingAmount()) {
            result.put("valid", false);
            result.put("message", "Giá trị đơn tối thiểu để áp dụng mã là " + promo.getMinBookingAmount() + "đ.");
            return result;
        }

        // Tính số tiền giảm giá [FR-53]
        long discount = 0L;
        if (bookingAmount != null && bookingAmount > 0 && promo.getDiscountPercent() != null) {
            discount = (long) (bookingAmount * (promo.getDiscountPercent() / 100.0));
            if (promo.getMaxDiscountAmount() != null && promo.getMaxDiscountAmount() > 0) {
                discount = Math.min(discount, promo.getMaxDiscountAmount());
            }
        }

        result.put("valid", true);
        result.put("message", "Áp dụng mã khuyến mãi thành công!");
        result.put("code", promo.getCode());
        result.put("discountPercent", promo.getDiscountPercent());
        result.put("discountAmount", discount);
        result.put("finalAmount", Math.max(0, (bookingAmount != null ? bookingAmount : 0) - discount));
        result.put("promotion", promo);

        return result;
    }

    // FR-54: Thêm mới hoặc cập nhật khuyến mãi
    @Transactional
    public Promotion savePromotion(Promotion promotion) {
        if (promotion.getCode() == null || promotion.getCode().isBlank()) {
            throw new IllegalArgumentException("Mã khuyến mãi không được để trống.");
        }
        promotion.setCode(promotion.getCode().trim().toUpperCase());
        return promotionRepository.save(promotion);
    }

    // FR-54: Xóa khuyến mãi
    @Transactional
    public void deletePromotion(Long id) {
        promotionRepository.deleteById(id);
    }

    // FR-55: Tăng lượt sử dụng
    @Transactional
    public void incrementUsedCount(String code) {
        promotionRepository.findByCodeIgnoreCase(code).ifPresent(p -> {
            p.setUsedCount((p.getUsedCount() != null ? p.getUsedCount() : 0) + 1);
            promotionRepository.save(p);
        });
    }

    // FR-55: Thống kê hiệu quả khuyến mãi
    public Map<String, Object> getPromotionStats() {
        List<Promotion> all = promotionRepository.findAll();
        long totalPromotions = all.size();
        long activeCount = all.stream().filter(p -> Boolean.TRUE.equals(p.getIsActive())).count();
        long totalUsed = all.stream().mapToLong(p -> p.getUsedCount() != null ? p.getUsedCount() : 0).sum();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalPromotions", totalPromotions);
        stats.put("activePromotions", activeCount);
        stats.put("totalRedemptions", totalUsed);
        stats.put("promotions", all);
        return stats;
    }
}

