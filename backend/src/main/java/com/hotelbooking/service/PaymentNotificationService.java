
package com.hotelbooking.service;

import com.hotelbooking.model.Booking;
import com.hotelbooking.model.CustomerNotification;
import com.hotelbooking.repository.CustomerNotificationRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentNotificationService {

    private final CustomerNotificationRepository notificationRepository;

    public PaymentNotificationService(
            CustomerNotificationRepository notificationRepository) {

        this.notificationRepository = notificationRepository;
    }

    // FR-92: Tao noi dung thong bao thanh toan thanh cong
    public String buildPaymentSuccessNotification(
            String bookingId,
            Long amount) {

        validatePaymentInfo(bookingId, amount);

        return "Thanh toan thanh cong!\n"
                + "Ma dat phong: " + bookingId + "\n"
                + "So tien: " + amount + " VND\n"
                + "Cam on ban da su dung dich vu.";
    }

    // FR-92: Tao noi dung thong bao thanh toan that bai
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

    // FR-92: Luu thong bao sau khi giao dich duoc xac nhan thanh cong
    @Transactional
    public CustomerNotification savePaymentSuccessNotification(
            Booking booking,
            Long amount) {

        validateBooking(booking);
        String message = buildPaymentSuccessNotification(
                booking.getBookingId(), amount
        );

        CustomerNotification notification =
                createNotification(
                        booking,
                        "PAYMENT_SUCCESS",
                        "Thanh toan thanh cong",
                        message
                );

        return notificationRepository.save(notification);
    }

    // FR-92: Luu thong bao sau khi giao dich duoc xac nhan that bai
    @Transactional
    public CustomerNotification savePaymentFailureNotification(
            Booking booking) {

        validateBooking(booking);

        String message = buildPaymentFailureNotification(
                booking.getBookingId()
        );

        CustomerNotification notification =
                createNotification(
                        booking,
                        "PAYMENT_FAILURE",
                        "Thanh toan that bai",
                        message
                );

        return notificationRepository.save(notification);
    }

    // Tao doi tuong thong bao chung
    private CustomerNotification createNotification(
            Booking booking,
            String type,
            String title,
            String message) {

        CustomerNotification notification =
                new CustomerNotification();

        notification.setBookingId(booking.getBookingId());
        notification.setUserId(booking.getUserId());
        notification.setRecipientEmail(booking.getGuestEmail());
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRead(false);

        return notification;
    }

    // Kiem tra thong tin dat phong
    private void validateBooking(Booking booking) {

        if (booking == null
                || booking.getBookingId() == null
                || booking.getBookingId().isBlank()) {

            throw new IllegalArgumentException(
                    "Thong tin dat phong khong hop le."
            );
        }
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
