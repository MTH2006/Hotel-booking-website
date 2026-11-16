
package com.hotelbooking.service;

import com.hotelbooking.model.Booking;
import com.hotelbooking.model.CustomerNotification;
import com.hotelbooking.repository.CustomerNotificationRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BookingConfirmationNotificationPersistenceServiceTest {

    private CustomerNotificationRepository repository;
    private BookingConfirmationNotificationPersistenceService service;

    @BeforeEach
    void setUp() {

        repository = mock(CustomerNotificationRepository.class);

        BookingNotificationService notificationService =
                new BookingNotificationService();

        service = new BookingConfirmationNotificationPersistenceService(
                notificationService,
                repository
        );

        // Gia lap viec luu thong bao vao database
        when(repository.save(any(CustomerNotification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    // FR-91: Luu thong bao xac nhan dat phong thanh cong
    @Test
    void shouldSaveBookingConfirmationSuccessfully() {

        Booking booking = createBooking();

        CustomerNotification result =
                service.saveBookingConfirmationNotification(booking);

        assertNotNull(result);
        assertEquals("BOOKING_CONFIRMED", result.getType());
        assertEquals("HB-123456", result.getBookingId());
        assertEquals(10L, result.getUserId());

        assertEquals(
                "customer@example.com",
                result.getRecipientEmail()
        );

        assertEquals(
                "Xac nhan dat phong thanh cong",
                result.getTitle()
        );

        assertFalse(result.isRead());

        verify(repository, times(1))
                .save(any(CustomerNotification.class));
    }

    // FR-91: Noi dung thong bao phai co thong tin don
    @Test
    void shouldContainCorrectBookingInformation() {

        Booking booking = createBooking();

        CustomerNotification result =
                service.saveBookingConfirmationNotification(booking);

        String message = result.getMessage();

        assertTrue(message.contains("HB-123456"));
        assertTrue(message.contains("Nguyen Van A"));
        assertTrue(message.contains("Hotel Booking Demo"));
        assertTrue(message.contains("DELUXE"));
        assertTrue(message.contains("2026-12-01"));
        assertTrue(message.contains("2026-12-05"));
        assertTrue(message.contains("1500000"));
    }

    // FR-91: Khong luu thong bao khi booking null
    @Test
    void shouldRejectNullBooking() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.saveBookingConfirmationNotification(null)
        );

        verify(repository, never())
                .save(any(CustomerNotification.class));
    }

    // FR-91: Khong luu khi ma dat phong rong
    @Test
    void shouldRejectBlankBookingId() {

        Booking booking = createBooking();
        booking.setBookingId("   ");

        assertThrows(
                IllegalArgumentException.class,
                () -> service.saveBookingConfirmationNotification(booking)
        );

        verify(repository, never())
                .save(any(CustomerNotification.class));
    }

    // FR-91: Ho tro khach dat phong khong co tai khoan
    @Test
    void shouldSupportGuestBooking() {

        Booking booking = createBooking();
        booking.setUserId(null);

        CustomerNotification result =
                service.saveBookingConfirmationNotification(booking);

        assertNull(result.getUserId());

        assertEquals(
                "customer@example.com",
                result.getRecipientEmail()
        );

        verify(repository, times(1))
                .save(any(CustomerNotification.class));
    }

    // FR-91: Loi database phai duoc thong bao cho ben goi
    @Test
    void shouldPropagateDatabaseError() {

        Booking booking = createBooking();

        when(repository.save(any(CustomerNotification.class)))
                .thenThrow(new RuntimeException("Database unavailable"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> service.saveBookingConfirmationNotification(booking)
        );

        assertEquals(
                "Database unavailable",
                exception.getMessage()
        );
    }

    // Tao du lieu booking mau
    private Booking createBooking() {

        Booking booking = new Booking();

        booking.setBookingId("HB-123456");
        booking.setUserId(10L);

        booking.setGuestName("Nguyen Van A");
        booking.setGuestEmail("customer@example.com");
        booking.setHotelName("Hotel Booking Demo");

        booking.setRoomType("DELUXE");

        booking.setCheckIn(
                LocalDate.of(2026, 12, 1)
        );

        booking.setCheckOut(
                LocalDate.of(2026, 12, 5)
        );

        booking.setTotalPrice(1500000L);
        booking.setStatus("CONFIRMED");

        return booking;
    }
}
