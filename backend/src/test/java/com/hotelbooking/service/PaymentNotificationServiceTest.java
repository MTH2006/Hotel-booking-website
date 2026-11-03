
package com.hotelbooking.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PaymentNotificationServiceTest {

    private final PaymentNotificationService notificationService =
            new PaymentNotificationService();

    // FR-92: Kiem tra thong bao thanh toan thanh cong
    @Test
    void shouldBuildPaymentSuccessNotification() {

        String message =
                notificationService.buildPaymentSuccessNotification(
                        "HB-123456",
                        1500000L
                );

        assertNotNull(message);
        assertTrue(message.contains("Thanh toan thanh cong"));
        assertTrue(message.contains("HB-123456"));
        assertTrue(message.contains("1500000"));
    }

    // FR-92: Kiem tra thong bao thanh toan that bai
    @Test
    void shouldBuildPaymentFailureNotification() {

        String message =
                notificationService.buildPaymentFailureNotification(
                        "HB-123456"
                );

        assertNotNull(message);
        assertTrue(message.contains("Thanh toan that bai"));
        assertTrue(message.contains("HB-123456"));
    }

    // FR-92: Tu choi so tien khong hop le
    @Test
    void shouldRejectInvalidPaymentAmount() {

        assertThrows(
                IllegalArgumentException.class,
                () -> notificationService
                        .buildPaymentSuccessNotification(
                                "HB-123456",
                                -1000L
                        )
        );
    }

    // FR-92: Tu choi ma dat phong khong hop le
    @Test
    void shouldRejectInvalidBookingId() {

        assertThrows(
                IllegalArgumentException.class,
                () -> notificationService
                        .buildPaymentFailureNotification("")
        );
    }
}
