
package com.hotelbooking.service;

import com.hotelbooking.model.Booking;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

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

    // FR-93: Thong bao khi doi ngay nhan phong
    @Test
    void shouldNotifyCheckInDateChange() {

        Booking before = createBooking();
        Booking after = createBooking();

        after.setCheckIn(LocalDate.of(2026, 12, 3));

        String message =
                notificationService.buildBookingUpdateNotification(
                        before, after
                );

        assertTrue(message.contains("Ngay nhan phong"));
        assertTrue(message.contains("2026-12-01"));
        assertTrue(message.contains("2026-12-03"));
    }

    // FR-93: Thong bao khi doi ngay tra phong
    @Test
    void shouldNotifyCheckOutDateChange() {

        Booking before = createBooking();
        Booking after = createBooking();

        after.setCheckOut(LocalDate.of(2026, 12, 7));

        String message =
                notificationService.buildBookingUpdateNotification(
                        before, after
                );

        assertTrue(message.contains("Ngay tra phong"));
        assertTrue(message.contains("2026-12-05"));
        assertTrue(message.contains("2026-12-07"));
    }

    // FR-93: Thong bao khi doi loai phong
    @Test
    void shouldNotifyRoomTypeChange() {

        Booking before = createBooking();
        Booking after = createBooking();

        after.setRoomType("SUITE");

        String message =
                notificationService.buildBookingUpdateNotification(
                        before, after
                );

        assertTrue(message.contains("Loai phong"));
        assertTrue(message.contains("DELUXE"));
        assertTrue(message.contains("SUITE"));
    }

    // FR-93: Thong bao khi trang thai don thay doi
    @Test
    void shouldNotifyBookingStatusChange() {

        Booking before = createBooking();
        Booking after = createBooking();

        after.setStatus("CANCELLED");

        String message =
                notificationService.buildBookingUpdateNotification(
                        before, after
                );

        assertTrue(message.contains("Trang thai"));
        assertTrue(message.contains("CONFIRMED"));
        assertTrue(message.contains("CANCELLED"));
    }

    // FR-93: Thong bao khi tong tien thay doi
    @Test
    void shouldNotifyTotalPriceChange() {

        Booking before = createBooking();
        Booking after = createBooking();

        after.setTotalPrice(2000000L);

        String message =
                notificationService.buildBookingUpdateNotification(
                        before, after
                );

        assertTrue(message.contains("Tong tien"));
        assertTrue(message.contains("1500000"));
        assertTrue(message.contains("2000000"));
    }

    // FR-93: Khong thong bao khi khong co thay doi
    @Test
    void shouldRejectUnchangedBooking() {

        Booking before = createBooking();
        Booking after = createBooking();

        assertThrows(
                IllegalArgumentException.class,
                () -> notificationService
                        .buildBookingUpdateNotification(
                                before, after
                        )
        );
    }

    // FR-93: Khong so sanh hai don khac nhau
    @Test
    void shouldRejectDifferentBookingIds() {

        Booking before = createBooking();
        Booking after = createBooking();

        after.setBookingId("HB-999999");

        assertThrows(
                IllegalArgumentException.class,
                () -> notificationService
                        .buildBookingUpdateNotification(
                                before, after
                        )
        );
    }

    // FR-93: Tu choi du lieu truoc khi thay doi bi null
    @Test
    void shouldRejectNullPreviousBooking() {

        Booking after = createBooking();

        assertThrows(
                IllegalArgumentException.class,
                () -> notificationService
                        .buildBookingUpdateNotification(
                                null, after
                        )
        );
    }

    // FR-93: Thong bao nhieu thay doi cung luc
    @Test
    void shouldNotifyMultipleChanges() {

        Booking before = createBooking();
        Booking after = createBooking();

        after.setCheckIn(LocalDate.of(2026, 12, 2));
        after.setRoomType("SUITE");
        after.setTotalPrice(2500000L);

        String message =
                notificationService.buildBookingUpdateNotification(
                        before, after
                );

        assertTrue(message.contains("Ngay nhan phong"));
        assertTrue(message.contains("Loai phong"));
        assertTrue(message.contains("Tong tien"));

        // Dam bao du lieu goc khong bi thay doi
        assertEquals(
                LocalDate.of(2026, 12, 1),
                before.getCheckIn()
        );
        assertEquals("DELUXE", before.getRoomType());
        assertEquals(1500000L, before.getTotalPrice());
    }

    // Tao du lieu mau cho cac bai kiem thu
    private Booking createBooking() {

        Booking booking = new Booking();

        booking.setBookingId("HB-123456");
        booking.setGuestName("Nguyen Van A");
        booking.setHotelName("Hotel Booking Demo");

        booking.setCheckIn(LocalDate.of(2026, 12, 1));
        booking.setCheckOut(LocalDate.of(2026, 12, 5));

        booking.setRoomType("DELUXE");
        booking.setStatus("CONFIRMED");
        booking.setTotalPrice(1500000L);

        return booking;
    }
}
