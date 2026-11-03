
package com.hotelbooking.service;

import org.springframework.stereotype.Service;

@Service
public class PaymentNotificationService {

    // FR-92: Tao thong bao thanh toan thanh cong
    public String buildPaymentSuccessNotification(
            String bookingId,
            Long amount) {

        validatePaymentInfo(bookingId, amount);

        return "Thanh toan thanh cong!\n"
                + "Ma dat phong: " + bookingId + "\n"
                + "So tien: " + amount + " VND\n"
                + "Cam on ban da su dung dich vu.";
    }

    // FR-92: Tao thong bao thanh toan that bai
    public String buildPaymentFailureNotification(
            String bookingId) {

        if (bookingId == null || bookingId.isBlank()) {
            throw new IllegalArgumentException(
                "Ma dat phong khong hop le."
            );
        }

        return "Thanh toan that bai!\n"
                + "Ma dat phong: " + bookingId + "\n"
                + "Vui long kiem tra va thu lai.";
    }

    // Kiem tra thong tin thanh toan
    private void validatePaymentInfo(
            String bookingId,
            Long amount) {

        if (bookingId == null || bookingId.isBlank()) {
            throw new IllegalArgumentException(
                "Ma dat phong khong hop le."
            );
        }

        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException(
                "So tien thanh toan khong hop le."
            );
        }
    }
}
