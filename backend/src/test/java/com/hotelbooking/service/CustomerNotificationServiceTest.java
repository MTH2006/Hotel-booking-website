
package com.hotelbooking.service;

import com.hotelbooking.model.CustomerNotification;
import com.hotelbooking.repository.CustomerNotificationRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomerNotificationServiceTest {

    private CustomerNotificationRepository repository;
    private CustomerNotificationService service;

    @BeforeEach
    void setUp() {
        repository = mock(CustomerNotificationRepository.class);
        service = new CustomerNotificationService(repository);
    }

    @Test
    void shouldGetNotificationsOfCurrentUser() {
        CustomerNotification notification =
                createNotification(10L);

        when(repository.findByUserIdOrderByCreatedAtDesc(10L))
                .thenReturn(List.of(notification));

        List<CustomerNotification> result =
                service.getNotifications(10L);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getUserId());

        verify(repository)
                .findByUserIdOrderByCreatedAtDesc(10L);
    }

    @Test
    void shouldRejectInvalidUserId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.getNotifications(0L)
        );

        verifyNoInteractions(repository);
    }

    @Test
    void shouldMarkOwnNotificationAsRead() {
        CustomerNotification notification =
                createNotification(10L);

        when(repository.findById(7L))
                .thenReturn(Optional.of(notification));

        when(repository.save(notification))
                .thenReturn(notification);

        Optional<CustomerNotification> result =
                service.markAsRead(7L, 10L);

        assertTrue(result.isPresent());
        assertTrue(result.get().isRead());

        verify(repository).save(notification);
    }

    @Test
    void shouldRejectOtherUsersNotification() {
        CustomerNotification notification =
                createNotification(20L);

        when(repository.findById(7L))
                .thenReturn(Optional.of(notification));

        Optional<CustomerNotification> result =
                service.markAsRead(7L, 10L);

        assertTrue(result.isEmpty());
        assertFalse(notification.isRead());

        verify(repository, never())
                .save(any(CustomerNotification.class));
    }

    @Test
    void shouldReturnEmptyWhenNotificationNotFound() {
        when(repository.findById(99L))
                .thenReturn(Optional.empty());

        Optional<CustomerNotification> result =
                service.markAsRead(99L, 10L);

        assertTrue(result.isEmpty());

        verify(repository, never())
                .save(any(CustomerNotification.class));
    }

    @Test
    void shouldRejectInvalidNotificationId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.markAsRead(0L, 10L)
        );

        verifyNoInteractions(repository);
    }

    private CustomerNotification createNotification(Long userId) {
        CustomerNotification notification =
                new CustomerNotification();

        notification.setUserId(userId);
        notification.setBookingId("HB-123456");
        notification.setType("PAYMENT_SUCCESS");
        notification.setTitle("Thanh toan thanh cong");
        notification.setMessage("Thanh toan thanh cong.");
        notification.setRead(false);

        return notification;
    }
}
