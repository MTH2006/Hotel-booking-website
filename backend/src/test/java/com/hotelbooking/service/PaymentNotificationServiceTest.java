
package com.hotelbooking.service;

import com.hotelbooking.model.Booking;
import com.hotelbooking.model.CustomerNotification;
import com.hotelbooking.repository.CustomerNotificationRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PaymentNotificationServiceTest {

    private CustomerNotificationRepository repository;
    private PaymentNotificationService notificationService;

    @BeforeEach
    void setUp() {
        repository = mock(CustomerNotificationRepository.class);

        notificationService =
                new PaymentNotificationService(repository);

        when(repository.save(any(CustomerNotification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldBuildPaymentSuccessNotification() {

        String message =
                notificationService.buildPaymentSuccessNotification(
                        "HB-123456", 1500000L
                );

        assertNotNull(message);
        assertTrue(message.contains("Thanh toan thanh cong"));
        assertTrue(message.contains("HB-123456"));
        assertTrue(message.contains("1500000"));
    }

    // FR-92: Kiem tra thong bao thanh toan that bai
    @Test
    void shouldBuildPaymentFailureNotification() {

        String message =
                notificationService.buildPaymentFailureNotification(
                        "HB-123456"
                );

        assertNotNull(message);
        assertTrue(message.contains("Thanh toan that bai"));
        assertTrue(message.contains("HB-123456"));
    }

    // FR-92: Tu choi so tien khong hop le
    @Test
    void shouldRejectInvalidPaymentAmount() {

        assertThrows(
                IllegalArgumentException.class,
                () -> notificationService
                        .buildPaymentSuccessNotification(
                                "HB-123456", -1000L
                        )
        );
    }

    // FR-92: Tu choi ma dat phong khong hop le
    @Test
    void shouldRejectInvalidBookingId() {

        assertThrows(
                IllegalArgumentException.class,
                () -> notificationService
                        .buildPaymentFailureNotification("")
        );
    }

    @Test
    void shouldSavePaymentSuccessNotification() {

        Booking booking = createBooking();

        CustomerNotification result =
                notificationService.savePaymentSuccessNotification(
                        booking, 1500000L
                );

        assertEquals("PAYMENT_SUCCESS", result.getType());
        assertEquals("HB-123456", result.getBookingId());
        assertEquals(10L, result.getUserId());
        assertEquals("customer@example.com",
                result.getRecipientEmail());
        assertFalse(result.isRead());

        verify(repository, times(1))
                .save(any(CustomerNotification.class));
    }

    @Test
    void shouldSavePaymentFailureNotification() {

        Booking booking = createBooking();

        CustomerNotification result =
                notificationService.savePaymentFailureNotification(
                        booking
                );

        assertEquals("PAYMENT_FAILURE", result.getType());
        assertTrue(result.getMessage()
                .contains("Thanh toan that bai"));

        verify(repository, times(1))
                .save(any(CustomerNotification.class));
    }

    @Test
    void shouldNotSaveInvalidPaymentNotification() {

        Booking booking = createBooking();

        assertThrows(
                IllegalArgumentException.class,
                () -> notificationService
                        .savePaymentSuccessNotification(
                                booking, -100L
                        )
        );

        verify(repository, never())
                .save(any(CustomerNotification.class));
    }

    @Test
    void shouldRejectNullBooking() {

        assertThrows(
                IllegalArgumentException.class,
                () -> notificationService
                        .savePaymentFailureNotification(null)
        );

        verify(repository, never())
                .save(any(CustomerNotification.class));
    }

    private Booking createBooking() {

        Booking booking = new Booking();

        booking.setBookingId("HB-123456");
        booking.setUserId(10L);
        booking.setGuestEmail("customer@example.com");

        return booking;
    }
}
