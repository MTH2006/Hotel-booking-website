
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

class BookingChangeNotificationPersistenceServiceTest {

    private CustomerNotificationRepository repository;
    private BookingChangeNotificationPersistenceService service;

    @BeforeEach
    void setUp() {

        repository = mock(CustomerNotificationRepository.class);

        BookingChangeNotificationService notificationService =
                new BookingChangeNotificationService();

        service = new BookingChangeNotificationPersistenceService(
                notificationService,
                repository
        );

        // Gia lap thao tac luu thong bao
        when(repository.save(any(CustomerNotification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    // FR-93: Luu thong bao khi thong tin dat phong thay doi
    @Test
    void shouldSaveBookingUpdateNotification() {

        Booking before = createBooking();
        Booking after = createBooking();

        after.setCheckIn(LocalDate.of(2026, 12, 3));

        CustomerNotification result =
                service.saveBookingUpdateNotification(before, after);

        assertNotNull(result);
        assertEquals("BOOKING_UPDATED", result.getType());
        assertEquals("HB-123456", result.getBookingId());
        assertEquals(10L, result.getUserId());
        assertEquals("customer@example.com",
                result.getRecipientEmail());

        assertTrue(result.getMessage()
                .contains("Ngay nhan phong"));

        assertFalse(result.isRead());

        verify(repository, times(1))
                .save(any(CustomerNotification.class));
    }

    // FR-93: Luu thong bao sau khi don da bi huy
    @Test
    void shouldSaveBookingCancellationNotification() {

        Booking booking = createBooking();
        booking.setStatus("CANCELLED");

        CustomerNotification result =
                service.saveBookingCancellationNotification(booking);

        assertNotNull(result);
        assertEquals("BOOKING_CANCELLED", result.getType());
        assertEquals("HB-123456", result.getBookingId());

        assertTrue(result.getMessage().contains("CANCELLED"));
        assertFalse(result.isRead());

        verify(repository, times(1))
                .save(any(CustomerNotification.class));
    }

    // FR-93: Khong luu thong bao neu du lieu khong thay doi
    @Test
    void shouldNotSaveUnchangedBooking() {

        Booking before = createBooking();
        Booking after = createBooking();

        assertThrows(
                IllegalArgumentException.class,
                () -> service.saveBookingUpdateNotification(
                        before, after
                )
        );

        verify(repository, never())
                .save(any(CustomerNotification.class));
    }

    // FR-93: Khong luu thong bao huy neu don chua huy
    @Test
    void shouldNotSaveNonCancelledBooking() {

        Booking booking = createBooking();
        booking.setStatus("CONFIRMED");

        assertThrows(
                IllegalArgumentException.class,
                () -> service.saveBookingCancellationNotification(
                        booking
                )
        );

        verify(repository, never())
                .save(any(CustomerNotification.class));
    }

    // FR-93: Khong luu thong bao cho hai don khac nhau
    @Test
    void shouldRejectDifferentBookingIds() {

        Booking before = createBooking();
        Booking after = createBooking();

        after.setBookingId("HB-999999");
        after.setRoomType("SUITE");

        assertThrows(
                IllegalArgumentException.class,
                () -> service.saveBookingUpdateNotification(
                        before, after
                )
        );

        verify(repository, never())
                .save(any(CustomerNotification.class));
    }

    // FR-93: Tu choi du lieu dat phong null
    @Test
    void shouldRejectNullBooking() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.saveBookingCancellationNotification(
                        null
                )
        );

        verify(repository, never())
                .save(any(CustomerNotification.class));
    }

    // FR-93: Loi luu tru phai duoc bao ve cho quy trinh goi
    @Test
    void shouldPropagatePersistenceFailure() {

        Booking booking = createBooking();
        booking.setStatus("CANCELLED");

        when(repository.save(any(CustomerNotification.class)))
                .thenThrow(new RuntimeException("Database unavailable"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> service.saveBookingCancellationNotification(
                        booking
                )
        );

        assertEquals("Database unavailable",
                exception.getMessage());
    }

    // Tao du lieu mau dung chung
    private Booking createBooking() {

        Booking booking = new Booking();

        booking.setBookingId("HB-123456");
        booking.setUserId(10L);

        booking.setGuestName("Nguyen Van A");
        booking.setGuestEmail("customer@example.com");
        booking.setHotelName("Hotel Booking Demo");

        booking.setCheckIn(LocalDate.of(2026, 12, 1));
        booking.setCheckOut(LocalDate.of(2026, 12, 5));

        booking.setRoomType("DELUXE");
        booking.setStatus("CONFIRMED");
        booking.setTotalPrice(1500000L);

        return booking;
    }
}
