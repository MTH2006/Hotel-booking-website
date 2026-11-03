package com.hotelbooking.controller;

import com.hotelbooking.model.Promotion;
import com.hotelbooking.model.User;
import com.hotelbooking.security.AccessControlService;
import com.hotelbooking.service.PromotionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class PromotionController {

    private final PromotionService promotionService;
    private final AccessControlService accessControl;

    public PromotionController(PromotionService promotionService, AccessControlService accessControl) {
        this.promotionService = promotionService;
        this.accessControl = accessControl;
    }

    // FR-51: Danh sách khuyến mãi đang hoạt động (công khai)
    @GetMapping("/promotions")
    public ResponseEntity<List<Promotion>> getActivePromotions() {
        return ResponseEntity.ok(promotionService.getActivePromotions());
    }

    // FR-51: Chi tiết 1 khuyến mãi
    @GetMapping("/promotions/{id}")
    public ResponseEntity<Promotion> getPromotionById(@PathVariable Long id) {
        return promotionService.getPromotionById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // FR-52 & FR-53: Áp dụng mã khuyến mãi
    @PostMapping("/promotions/apply")
    public ResponseEntity<Map<String, Object>> applyPromotion(@RequestBody Map<String, Object> payload) {
        String code = (String) payload.get("code");
        Number amount = (Number) payload.get("bookingAmount");
        Long bookingAmount = amount != null ? amount.longValue() : 0L;

        Map<String, Object> result = promotionService.applyPromotion(code, bookingAmount);
        boolean valid = Boolean.TRUE.equals(result.get("valid"));
        return valid ? ResponseEntity.ok(result) : ResponseEntity.badRequest().body(result);
    }

    // FR-54: Quản trị viên xem tất cả khuyến mãi
    @GetMapping("/admin/promotions")
    public ResponseEntity<List<Promotion>> getAllPromotionsForAdmin(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Optional<User> user = accessControl.authenticate(authorization);
        if (user.isEmpty() || !accessControl.isAdmin(user.get())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(promotionService.getAllPromotions());
    }

    // FR-54: Thêm mới khuyến mãi (Admin)
    @PostMapping("/admin/promotions")
    public ResponseEntity<?> createPromotion(
            @RequestBody Promotion promotion,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Optional<User> user = accessControl.authenticate(authorization);
        if (user.isEmpty() || !accessControl.isAdmin(user.get())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            promotion.setId(null);
            Promotion created = promotionService.savePromotion(promotion);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // FR-54: Cập nhật khuyến mãi (Admin)
    @PutMapping("/admin/promotions/{id}")
    public ResponseEntity<?> updatePromotion(
            @PathVariable Long id,
            @RequestBody Promotion update,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Optional<User> user = accessControl.authenticate(authorization);
        if (user.isEmpty() || !accessControl.isAdmin(user.get())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Optional<Promotion> existingOpt = promotionService.getPromotionById(id);
        if (existingOpt.isEmpty()) return ResponseEntity.notFound().build();

        Promotion existing = existingOpt.get();
        if (update.getCode() != null) existing.setCode(update.getCode());
        if (update.getTitle() != null) existing.setTitle(update.getTitle());
        if (update.getDiscountPercent() != null) existing.setDiscountPercent(update.getDiscountPercent());
        if (update.getMaxDiscountAmount() != null) existing.setMaxDiscountAmount(update.getMaxDiscountAmount());
        if (update.getMinBookingAmount() != null) existing.setMinBookingAmount(update.getMinBookingAmount());
        if (update.getStartDate() != null) existing.setStartDate(update.getStartDate());
        if (update.getEndDate() != null) existing.setEndDate(update.getEndDate());
        if (update.getUsageLimit() != null) existing.setUsageLimit(update.getUsageLimit());
        if (update.getIsActive() != null) existing.setIsActive(update.getIsActive());

        Promotion saved = promotionService.savePromotion(existing);
        return ResponseEntity.ok(saved);
    }

    // FR-54: Xóa khuyến mãi (Admin)
    @DeleteMapping("/admin/promotions/{id}")
    public ResponseEntity<Void> deletePromotion(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Optional<User> user = accessControl.authenticate(authorization);
        if (user.isEmpty() || !accessControl.isAdmin(user.get())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (promotionService.getPromotionById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        promotionService.deletePromotion(id);
        return ResponseEntity.noContent().build();
    }

    // FR-55: Thống kê hiệu quả khuyến mãi
    @GetMapping("/admin/promotions/statistics")
    public ResponseEntity<Map<String, Object>> getPromotionStats(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Optional<User> user = accessControl.authenticate(authorization);
        if (user.isEmpty() || !accessControl.isAdmin(user.get())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(promotionService.getPromotionStats());
    }
}

