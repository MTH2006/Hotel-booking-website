
package com.hotelbooking.service;

import com.hotelbooking.model.Booking;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BookingChangeNotificationServiceTest {

    private final BookingChangeNotificationService notificationService =
            new BookingChangeNotificationService();

    // FR-93: Kiem tra thong bao huy dat phong thanh cong
    @Test
    void shouldBuildCancellationNotification() {

        Booking booking = new Booking();

        booking.setBookingId("HB-123456");
        booking.setGuestName("Nguyen Van A");
        booking.setHotelName("Hotel Booking Demo");
        booking.setStatus("CANCELLED");

        String message =
                notificationService.buildCancellationNotification(booking);

        assertNotNull(message);
        assertTrue(message.contains("HB-123456"));
        assertTrue(message.contains("Nguyen Van A"));
        assertTrue(message.contains("Hotel Booking Demo"));
        assertTrue(message.contains("CANCELLED"));
    }

    // FR-93: Khong tao thong bao huy khi don chua bi huy
    @Test
    void shouldRejectNonCancelledBooking() {

        Booking booking = new Booking();

        booking.setBookingId("HB-123456");
        booking.setStatus("CONFIRMED");

        assertThrows(
                IllegalArgumentException.class,
                () -> notificationService
                        .buildCancellationNotification(booking)
        );
    }

    // FR-93: Tu choi don dat phong null
    @Test
    void shouldRejectNullBooking() {

        assertThrows(
                IllegalArgumentException.class,
                () -> notificationService
                        .buildCancellationNotification(null)
        );
    }

    // FR-93: Tu choi ma dat phong rong
    @Test
    void shouldRejectBlankBookingId() {

        Booking booking = new Booking();

        booking.setBookingId("");
        booking.setStatus("CANCELLED");

        assertThrows(
                IllegalArgumentException.class,
                () -> notificationService
                        .buildCancellationNotification(booking)
        );
    }
}

