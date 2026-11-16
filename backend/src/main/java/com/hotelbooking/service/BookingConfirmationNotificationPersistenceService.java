
package com.hotelbooking.service;

import com.hotelbooking.model.Booking;
import com.hotelbooking.model.CustomerNotification;
import com.hotelbooking.repository.CustomerNotificationRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingConfirmationNotificationPersistenceService {

    private final BookingNotificationService notificationService;
    private final CustomerNotificationRepository notificationRepository;

    public BookingConfirmationNotificationPersistenceService(
            BookingNotificationService notificationService,
            CustomerNotificationRepository notificationRepository) {

        this.notificationService = notificationService;
        this.notificationRepository = notificationRepository;
    }

    // FR-91: Luu thong bao xac nhan dat phong thanh cong
    @Transactional
    public CustomerNotification saveBookingConfirmationNotification(
            Booking booking) {

        // Kiem tra thong tin don dat phong
        if (booking == null
                || booking.getBookingId() == null
                || booking.getBookingId().isBlank()) {

            throw new IllegalArgumentException(
                    "Thong tin dat phong khong hop le."
            );
        }

        // Tao noi dung bang Service FR-91 da co
        String message =
                notificationService.buildBookingConfirmation(booking);

        // Tao thong bao moi
        CustomerNotification notification =
                new CustomerNotification();

        // Thong tin don dat phong
        notification.setBookingId(booking.getBookingId());

        // Thong tin nguoi nhan
        notification.setUserId(booking.getUserId());
        notification.setRecipientEmail(booking.getGuestEmail());

        // Noi dung thong bao
        notification.setType("BOOKING_CONFIRMED");
        notification.setTitle("Xac nhan dat phong thanh cong");
        notification.setMessage(message);

        // Mac dinh chua doc
        notification.setRead(false);

        // Luu vao PostgreSQL
        return notificationRepository.save(notification);
    }
}

