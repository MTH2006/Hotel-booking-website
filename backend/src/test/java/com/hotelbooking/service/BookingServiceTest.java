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
}

