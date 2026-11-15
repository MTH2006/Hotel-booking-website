
package com.hotelbooking.service;

import com.hotelbooking.model.Booking;
import com.hotelbooking.model.CustomerNotification;
import com.hotelbooking.repository.CustomerNotificationRepository;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class NotificationDatabaseIntegrationTest {

    @Autowired
    private BookingConfirmationNotificationPersistenceService bookingService;

    @Autowired
    private PaymentNotificationService paymentService;

    @Autowired
    private BookingChangeNotificationPersistenceService changeService;

    @Autowired
    private CustomerNotificationRepository repository;

    @Autowired
    private EntityManager entityManager;

    private Booking createBooking() {
        Booking booking = new Booking();

        booking.setBookingId(
                "TEST-" + UUID.randomUUID().toString().substring(0, 8)
        );

        // Khong gan ID nguoi dung that trong database
        booking.setUserId(null);
        booking.setGuestName("Integration Test");
        booking.setGuestEmail("integration@example.com");
        booking.setHotelName("Test Hotel");
        booking.setRoomType("DELUXE");
        booking.setCheckIn(LocalDate.of(2026, 12, 1));
        booking.setCheckOut(LocalDate.of(2026, 12, 3));
        booking.setTotalPrice(1500000L);
        booking.setStatus("CONFIRMED");

        return booking;
    }

    private CustomerNotification readFromDatabase(
            CustomerNotification notification) {

        assertNotNull(notification.getId());

        // Bat Hibernate ghi SQL xuong PostgreSQL
        entityManager.flush();

        // Xoa cache JPA de doc lai tu database
        entityManager.clear();

        return repository.findById(notification.getId())
                .orElseThrow();
    }

    @Test
    void fr91ShouldPersistBookingConfirmation() {
        Booking booking = createBooking();

        CustomerNotification saved =
                bookingService.saveBookingConfirmationNotification(booking);

        CustomerNotification found = readFromDatabase(saved);

        assertEquals("BOOKING_CONFIRMED", found.getType());
        assertEquals(booking.getBookingId(), found.getBookingId());
        assertFalse(found.isRead());
        assertNotNull(found.getCreatedAt());
    }

    @Test
    void fr92ShouldPersistPaymentSuccess() {
        Booking booking = createBooking();

        CustomerNotification saved =
                paymentService.savePaymentSuccessNotification(
                        booking, 1500000L
                );

        CustomerNotification found = readFromDatabase(saved);

        assertEquals("PAYMENT_SUCCESS", found.getType());
        assertTrue(found.getMessage().contains("1500000"));
    }

    @Test
    void fr92ShouldPersistPaymentFailure() {
        Booking booking = createBooking();

        CustomerNotification saved =
                paymentService.savePaymentFailureNotification(booking);

        CustomerNotification found = readFromDatabase(saved);

        assertEquals("PAYMENT_FAILURE", found.getType());
        assertEquals(booking.getBookingId(), found.getBookingId());
    }

    @Test
    void fr93ShouldPersistBookingUpdate() {
        Booking before = createBooking();
        Booking after = createBooking();

        after.setBookingId(before.getBookingId());
        after.setCheckIn(LocalDate.of(2026, 12, 2));

        CustomerNotification saved =
                changeService.saveBookingUpdateNotification(before, after);

        CustomerNotification found = readFromDatabase(saved);

        assertEquals("BOOKING_UPDATED", found.getType());
        assertTrue(found.getMessage().contains("2026-12-02"));
    }

    @Test
    void fr93ShouldPersistBookingCancellation() {
        Booking booking = createBooking();
        booking.setStatus("CANCELLED");

        CustomerNotification saved =
                changeService.saveBookingCancellationNotification(booking);

        CustomerNotification found = readFromDatabase(saved);

        assertEquals("BOOKING_CANCELLED", found.getType());
        assertFalse(found.isRead());
    }
}
