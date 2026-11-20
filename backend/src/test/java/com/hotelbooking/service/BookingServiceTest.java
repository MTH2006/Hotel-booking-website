package com.hotelbooking.service;

import com.hotelbooking.model.Booking;
import com.hotelbooking.model.Hotel;
import com.hotelbooking.model.Room;
import com.hotelbooking.repository.BookingRepository;
import com.hotelbooking.repository.HotelRepository;
import com.hotelbooking.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class BookingServiceTest {

    private BookingRepository bookingRepository;
    private HotelRepository hotelRepository;
    private RoomRepository roomRepository;
    private BookingNotificationService notificationService;
    private BookingChangeNotificationService changeNotificationService;
    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        bookingRepository = mock(BookingRepository.class);
        hotelRepository = mock(HotelRepository.class);
        roomRepository = mock(RoomRepository.class);
        notificationService = mock(BookingNotificationService.class);
        changeNotificationService = mock(BookingChangeNotificationService.class);

        bookingService = new BookingService(
                bookingRepository,
                hotelRepository,
                roomRepository,
                Optional.of(notificationService),
                Optional.of(changeNotificationService)
        );
    }

    @Test
    void testCreateBookingSuccess() {
        Booking booking = new Booking();
        booking.setHotelId(1L);
        booking.setRoomType("Deluxe King");
        booking.setCheckIn(LocalDate.now().plusDays(2));
        booking.setCheckOut(LocalDate.now().plusDays(5)); // 3 nights
        booking.setGuestName("Nguyen Van A");
        booking.setGuestEmail("an@example.com");
        booking.setGuestPhone("0901234567");

        Hotel hotel = new Hotel();
        hotel.setId(1L);
        hotel.setName("Grand Vista Hotel");
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel));

        Room room = new Room(10L, 1L, "Deluxe King", 1000000L, 2, 5, "Nice room");
        when(roomRepository.findByHotelIdAndRoomType(1L, "Deluxe King")).thenReturn(Optional.of(room));
        when(bookingRepository.countOverlappingBookings(eq(1L), eq("Deluxe King"), any(), any())).thenReturn(1L);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            b.setId(100L);
            return b;
        });

        Booking result = bookingService.createBooking(booking);

        assertNotNull(result);
        assertEquals("Grand Vista Hotel", result.getHotelName());
        assertEquals(3000000L, result.getTotalPrice()); // 3 nights * 1000000
        assertEquals(Booking.BookingStatus.CONFIRMED, result.getStatus());
        assertTrue(result.getBookingId().startsWith("HB-"));
    }

    @Test
    void testCreateBookingInvalidDates() {
        Booking booking = new Booking();
        booking.setCheckIn(LocalDate.now().plusDays(5));
        booking.setCheckOut(LocalDate.now().plusDays(2)); // Check-out before check-in

        assertThrows(IllegalArgumentException.class, () -> {
            bookingService.createBooking(booking);
        });
    }

    @Test
    void testCreateBookingOverbooking() {
        Booking booking = new Booking();
        booking.setHotelId(1L);
        booking.setRoomType("Deluxe King");
        booking.setCheckIn(LocalDate.now().plusDays(2));
        booking.setCheckOut(LocalDate.now().plusDays(4));

        Room room = new Room(10L, 1L, "Deluxe King", 1000000L, 2, 3, "Room");
        when(roomRepository.findByHotelIdAndRoomType(1L, "Deluxe King")).thenReturn(Optional.of(room));
        when(bookingRepository.countOverlappingBookings(eq(1L), eq("Deluxe King"), any(), any())).thenReturn(3L); // All 3 booked

        assertThrows(IllegalStateException.class, () -> {
            bookingService.createBooking(booking);
        });
    }

    @Test
    void testCancelBooking() {
        Booking booking = new Booking();
        booking.setBookingId("HB-123456");
        booking.setStatus(Booking.BookingStatus.CONFIRMED);

        when(bookingRepository.findByBookingId("HB-123456")).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

        boolean cancelled = bookingService.cancelBooking("HB-123456");
        assertTrue(cancelled);
        assertEquals(Booking.BookingStatus.CANCELLED, booking.getStatus());
    }
    
    // FR-91: Kiem tra tu dong luu thong bao khi dat phong thanh cong
    @Test
    void testFr91SaveBookingConfirmationNotification() {
        BookingConfirmationNotificationPersistenceService persistenceService =
                mock(BookingConfirmationNotificationPersistenceService.class);

        org.springframework.test.util.ReflectionTestUtils.setField(
                bookingService,
                "bookingConfirmationPersistenceService",
                persistenceService
        );

        Booking booking = new Booking();
        booking.setBookingId("HB-FR91-001");
        booking.setGuestName("Nguyen Van A");
        booking.setGuestEmail("customer@example.com");
        booking.setCheckIn(LocalDate.now().plusDays(2));
        booking.setCheckOut(LocalDate.now().plusDays(4));
        booking.setTotalPrice(2000000L);

        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Booking saved = bookingService.createBooking(booking);

        assertNotNull(saved);

        verify(persistenceService, times(1))
                .saveBookingConfirmationNotification(same(saved));
    }
    
    // FR-93: Luu thong bao khi huy don bang bookingId
    @Test
    void shouldSaveCancellationNotificationByBookingId() {
        Booking booking = new Booking();
        booking.setBookingId("HB-FR93-001");
        booking.setStatus(Booking.BookingStatus.CONFIRMED);

        BookingChangeNotificationPersistenceService persistenceService =
                mock(BookingChangeNotificationPersistenceService.class);

        org.springframework.test.util.ReflectionTestUtils.setField(
                bookingService,
                "bookingChangePersistenceService",
                persistenceService
        );

        when(bookingRepository.findByBookingId("HB-FR93-001"))
                .thenReturn(Optional.of(booking));

        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        boolean result = bookingService.cancelBooking("HB-FR93-001");

        assertTrue(result);
        assertEquals(
                Booking.BookingStatus.CANCELLED,
                booking.getStatus()
        );

        verify(persistenceService, times(1))
                .saveBookingCancellationNotification(same(booking));
    }

    // FR-93: Luu thong bao khi huy don bang ID
    @Test
    void shouldSaveCancellationNotificationById() {
        Booking booking = new Booking();
        booking.setId(201L);
        booking.setBookingId("HB-FR93-002");
        booking.setStatus(Booking.BookingStatus.CONFIRMED);

        BookingChangeNotificationPersistenceService persistenceService =
                mock(BookingChangeNotificationPersistenceService.class);

        org.springframework.test.util.ReflectionTestUtils.setField(
                bookingService,
                "bookingChangePersistenceService",
                persistenceService
        );

        when(bookingRepository.findById(201L))
                .thenReturn(Optional.of(booking));

        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Booking result = bookingService.cancelBooking(201L);

        assertNotNull(result);
        assertEquals(
                Booking.BookingStatus.CANCELLED,
                result.getStatus()
        );

        verify(persistenceService, times(1))
                .saveBookingCancellationNotification(same(result));
    }

    // FR-93: Cap nhat trang thai theo ID phai luu thong bao
    @Test
    void shouldNotifyWhenUpdatingStatusById() {
        Booking booking = new Booking();
        booking.setId(301L);
        booking.setBookingId("HB-FR93-301");
        booking.setStatus(Booking.BookingStatus.CONFIRMED);

        BookingChangeNotificationPersistenceService persistence =
                mock(BookingChangeNotificationPersistenceService.class);

        org.springframework.test.util.ReflectionTestUtils.setField(
                bookingService,
                "bookingChangePersistenceService",
                persistence
        );

        when(bookingRepository.findById(301L))
                .thenReturn(Optional.of(booking));

        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Booking result = bookingService.updateStatus(
                301L, "CHECKED_IN", null
        );

        assertEquals("CHECKED_IN", result.getStatus());

        verify(persistence, times(1))
                .saveBookingUpdateNotification(
                        argThat(before ->
                                "CONFIRMED".equals(before.getStatus())
                                && "HB-FR93-301".equals(
                                        before.getBookingId()
                                )
                        ),
                        same(result)
                );
    }

    // FR-93: Huy don theo bookingId phai luu thong bao huy
    @Test
    void shouldNotifyWhenCancellingByUpdateStatus() {
        Booking booking = new Booking();
        booking.setBookingId("HB-FR93-302");
        booking.setStatus(Booking.BookingStatus.CONFIRMED);

        BookingChangeNotificationPersistenceService persistence =
                mock(BookingChangeNotificationPersistenceService.class);

        org.springframework.test.util.ReflectionTestUtils.setField(
                bookingService,
                "bookingChangePersistenceService",
                persistence
        );

        when(bookingRepository.findByBookingId("HB-FR93-302"))
                .thenReturn(Optional.of(booking));

        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Booking result = bookingService.updateStatusByBookingId(
                "HB-FR93-302", "CANCELLED", "Khach yeu cau huy"
        );

        assertEquals(
                Booking.BookingStatus.CANCELLED,
                result.getStatus()
        );

        verify(persistence, times(1))
                .saveBookingCancellationNotification(same(result));

        verify(persistence, never())
                .saveBookingUpdateNotification(
                        any(Booking.class),
                        any(Booking.class)
                );
    }

    // FR-93: Khong tao thong bao neu trang thai khong doi
    @Test
    void shouldNotNotifyWhenStatusUnchanged() {
        Booking booking = new Booking();
        booking.setId(303L);
        booking.setBookingId("HB-FR93-303");
        booking.setStatus(Booking.BookingStatus.CONFIRMED);

        BookingChangeNotificationPersistenceService persistence =
                mock(BookingChangeNotificationPersistenceService.class);

        org.springframework.test.util.ReflectionTestUtils.setField(
                bookingService,
                "bookingChangePersistenceService",
                persistence
        );

        when(bookingRepository.findById(303L))
                .thenReturn(Optional.of(booking));

        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Booking result = bookingService.updateStatus(
                303L, "CONFIRMED", null
        );

        assertEquals("CONFIRMED", result.getStatus());

        verifyNoInteractions(persistence);
    }

}

