
package com.hotelbooking.service;

import com.hotelbooking.model.Booking;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class BookingNotificationServiceTest {

    private final BookingNotificationService notificationService =
            new BookingNotificationService();

    // FR-91: Kiem tra noi dung thong bao xac nhan dat phong
    @Test
    void shouldBuildBookingConfirmationSuccessfully() {

        // Tao du lieu gia lap, khong luu vao database
        Booking booking = new Booking();

        booking.setBookingId("HB-123456");
        booking.setGuestName("Nguyen Van A");
        booking.setHotelName("Hotel Booking Demo");
        booking.setRoomType("Deluxe");
        booking.setCheckIn(LocalDate.of(2026, 11, 1));
        booking.setCheckOut(LocalDate.of(2026, 11, 3));
        booking.setTotalPrice(1500000L);

        // Tao thong bao xac nhan
        String message =
                notificationService.buildBookingConfirmation(booking);

        // Kiem tra thong tin trong thong bao
        assertNotNull(message);
        assertTrue(message.contains("HB-123456"));
        assertTrue(message.contains("Nguyen Van A"));
        assertTrue(message.contains("Hotel Booking Demo"));
        assertTrue(message.contains("Deluxe"));
        assertTrue(message.contains("2026-11-01"));
        assertTrue(message.contains("2026-11-03"));
        assertTrue(message.contains("1500000"));
    }

    // FR-91: Khong chap nhan thong tin dat phong null
    @Test
    void shouldRejectNullBooking() {

        assertThrows(
                IllegalArgumentException.class,
                () -> notificationService.buildBookingConfirmation(null)
        );
    }
}
