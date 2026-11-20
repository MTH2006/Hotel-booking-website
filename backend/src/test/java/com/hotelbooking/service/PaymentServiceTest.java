
package com.hotelbooking.service;

import com.hotelbooking.model.Booking;
import com.hotelbooking.model.Payment;
import com.hotelbooking.repository.BookingRepository;
import com.hotelbooking.repository.PaymentRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PaymentServiceTest {

    private PaymentRepository paymentRepository;
    private BookingRepository bookingRepository;
    private PaymentNotificationService notificationService;
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentRepository = mock(PaymentRepository.class);
        bookingRepository = mock(BookingRepository.class);
        notificationService = mock(PaymentNotificationService.class);

        paymentService = new PaymentService(
                paymentRepository,
                bookingRepository,
                Optional.of(notificationService)
        );

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    // FR-92: Thanh toan thanh cong phai luu thong bao
    @Test
    void shouldSaveNotificationAfterSuccessfulPayment() {
        Booking booking = createBooking();

        when(bookingRepository.findByBookingId("HB-123456"))
                .thenReturn(Optional.of(booking));

        Payment result = paymentService.processPayment(
                "HB-123456",
                "BANK_TRANSFER",
                1500000L,
                "TEST"
        );

        assertNotNull(result);
        assertEquals("SUCCESS", result.getStatus());
        assertEquals(1500000L, result.getAmount());
        assertEquals("PAID", booking.getPaymentStatus());

        verify(paymentRepository, times(1))
                .save(any(Payment.class));

        verify(bookingRepository, times(1))
                .save(same(booking));

        verify(notificationService, times(1))
                .savePaymentSuccessNotification(
                        same(booking),
                        eq(1500000L)
                );
    }

    // FR-92: Su dung tong tien cua don neu amount la null
    @Test
    void shouldUseBookingTotalForPaymentNotification() {
        Booking booking = createBooking();

        when(bookingRepository.findByBookingId("HB-123456"))
                .thenReturn(Optional.of(booking));

        Payment result = paymentService.processPayment(
                "HB-123456",
                "BANK_TRANSFER",
                null,
                "TEST"
        );

        assertNotNull(result);
        assertEquals(1500000L, result.getAmount());

        verify(notificationService, times(1))
                .savePaymentSuccessNotification(
                        same(booking),
                        eq(1500000L)
                );
    }

    // FR-92: Khong tao thanh toan va thong bao neu don khong ton tai
    @Test
    void shouldNotNotifyWhenBookingDoesNotExist() {
        when(bookingRepository.findByBookingId("HB-UNKNOWN"))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> paymentService.processPayment(
                        "HB-UNKNOWN",
                        "BANK_TRANSFER",
                        1500000L,
                        "TEST"
                )
        );

        verify(paymentRepository, never())
                .save(any(Payment.class));

        verify(notificationService, never())
                .savePaymentSuccessNotification(
                        any(Booking.class),
                        anyLong()
                );
    }

    private Booking createBooking() {
        Booking booking = new Booking();
        booking.setBookingId("HB-123456");
        booking.setUserId(10L);
        booking.setGuestEmail("customer@example.com");
        booking.setTotalPrice(1500000L);
        return booking;
    }
}
