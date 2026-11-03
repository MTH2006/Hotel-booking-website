package com.hotelbooking.service;

import com.hotelbooking.model.Promotion;
import com.hotelbooking.repository.PromotionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class PromotionServiceTest {

    private PromotionRepository promotionRepository;
    private PromotionService promotionService;

    @BeforeEach
    void setUp() {
        promotionRepository = mock(PromotionRepository.class);
        promotionService = new PromotionService(promotionRepository);
    }

    @Test
    void testApplyValidPromotion() {
        Promotion promo = new Promotion(
                "HESTORA2026", "Giam 10%", 10.0, 500000L, 1000000L,
                LocalDate.now().minusDays(5), LocalDate.now().plusDays(20), 100
        );

        when(promotionRepository.findByCodeIgnoreCase("HESTORA2026")).thenReturn(Optional.of(promo));

        Map<String, Object> result = promotionService.applyPromotion("HESTORA2026", 2000000L);
        assertTrue((Boolean) result.get("valid"));
        assertEquals(200000L, result.get("discountAmount")); // 10% of 2M
        assertEquals(1800000L, result.get("finalAmount"));
    }

    @Test
    void testApplyExpiredPromotion() {
        Promotion promo = new Promotion(
                "OLDPROMO", "Het han", 20.0, 500000L, 500000L,
                LocalDate.now().minusDays(30), LocalDate.now().minusDays(2), 100
        );

        when(promotionRepository.findByCodeIgnoreCase("OLDPROMO")).thenReturn(Optional.of(promo));

        Map<String, Object> result = promotionService.applyPromotion("OLDPROMO", 1000000L);
        assertFalse((Boolean) result.get("valid"));
    }
}

