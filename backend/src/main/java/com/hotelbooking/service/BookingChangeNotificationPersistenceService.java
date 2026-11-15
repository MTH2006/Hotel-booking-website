
package com.hotelbooking.service;

import com.hotelbooking.model.Booking;
import com.hotelbooking.model.CustomerNotification;
import com.hotelbooking.repository.CustomerNotificationRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingChangeNotificationPersistenceService {

    private final BookingChangeNotificationService notificationService;
    private final CustomerNotificationRepository notificationRepository;

    public BookingChangeNotificationPersistenceService(
            BookingChangeNotificationService notificationService,
            CustomerNotificationRepository notificationRepository) {

        this.notificationService = notificationService;
        this.notificationRepository = notificationRepository;
    }

    // FR-93: Luu thong bao khi thong tin dat phong thay doi
    @Transactional
    public CustomerNotification saveBookingUpdateNotification(
            Booking before,
            Booking after) {

        // Tao noi dung thong bao tu hai phien ban dat phong
        String message =
                notificationService.buildBookingUpdateNotification(
                        before,
                        after
                );

        // Tao doi tuong thong bao
        CustomerNotification notification =
                createNotification(
                        after,
                        "BOOKING_UPDATED",
                        "Thong tin dat phong da thay doi",
                        message
                );

        // Luu thong bao vao PostgreSQL
        return notificationRepository.save(notification);
    }

    // FR-93: Luu thong bao khi don dat phong bi huy
    @Transactional
    public CustomerNotification saveBookingCancellationNotification(
            Booking booking) {

        // Chi tao thong bao neu don da o trang thai CANCELLED
        String message =
                notificationService.buildCancellationNotification(
                        booking
                );

        CustomerNotification notification =
                createNotification(
                        booking,
                        "BOOKING_CANCELLED",
                        "Don dat phong da bi huy",
                        message
                );

        // Luu thong bao vao PostgreSQL
        return notificationRepository.save(notification);
    }

    // Tao doi tuong thong bao dung chung cho FR-93
    private CustomerNotification createNotification(
            Booking booking,
            String type,
            String title,
            String message) {

        CustomerNotification notification =
                new CustomerNotification();

        // Thong tin don dat phong
        notification.setBookingId(booking.getBookingId());

        // Thong tin nguoi nhan
        notification.setUserId(booking.getUserId());
        notification.setRecipientEmail(booking.getGuestEmail());

        // Noi dung thong bao
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);

        // Mac dinh thong bao chua duoc doc
        notification.setRead(false);

        return notification;
    }
}
