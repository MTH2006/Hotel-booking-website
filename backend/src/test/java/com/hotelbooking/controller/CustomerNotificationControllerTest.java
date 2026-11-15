
package com.hotelbooking.controller;

import com.hotelbooking.model.CustomerNotification;
import com.hotelbooking.model.User;
import com.hotelbooking.security.AccessControlService;
import com.hotelbooking.service.CustomerNotificationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomerNotificationControllerTest {

    private CustomerNotificationService notificationService;
    private AccessControlService accessControl;
    private CustomerNotificationController controller;

    private User user;

    @BeforeEach
    void setUp() {

        notificationService = mock(CustomerNotificationService.class);
        accessControl = mock(AccessControlService.class);

        controller = new CustomerNotificationController(
                notificationService,
                accessControl
        );

        user = new User();
        user.setId(10L);
        user.setActive(true);
    }

    // FR-92: Khong cho xem thong bao khi chua dang nhap
    @Test
    void shouldRejectUnauthenticatedUser() {

        when(accessControl.authenticate(null))
                .thenReturn(Optional.empty());

        ResponseEntity<?> response =
                controller.getMyNotifications(null);

        assertEquals(401, response.getStatusCode().value());

        verifyNoInteractions(notificationService);
    }

    // FR-92: Khong cho tai khoan bi khoa xem thong bao
    @Test
    void shouldRejectInactiveUser() {

        user.setActive(false);

        when(accessControl.authenticate("Bearer valid"))
                .thenReturn(Optional.of(user));

        ResponseEntity<?> response =
                controller.getMyNotifications("Bearer valid");

        assertEquals(403, response.getStatusCode().value());

        verifyNoInteractions(notificationService);
    }

    // FR-92: Lay dung thong bao cua tai khoan dang nhap
    @Test
    void shouldGetOwnNotifications() {

        when(accessControl.authenticate("Bearer valid"))
                .thenReturn(Optional.of(user));

        CustomerNotification notification =
                createNotification();

        List<CustomerNotification> notifications =
                List.of(notification);

        when(notificationService.getNotifications(10L))
                .thenReturn(notifications);

        ResponseEntity<?> response =
                controller.getMyNotifications("Bearer valid");

        assertEquals(200, response.getStatusCode().value());
        assertEquals(notifications, response.getBody());

        verify(notificationService).getNotifications(10L);
    }

    // FR-92: Danh dau thong bao cua minh da doc
    @Test
    void shouldMarkOwnNotificationAsRead() {

        when(accessControl.authenticate("Bearer valid"))
                .thenReturn(Optional.of(user));

        CustomerNotification notification =
                createNotification();

        notification.setRead(true);

        when(notificationService.markAsRead(7L, 10L))
                .thenReturn(Optional.of(notification));

        ResponseEntity<?> response =
                controller.markNotificationAsRead(
                        7L, "Bearer valid"
                );

        assertEquals(200, response.getStatusCode().value());

        verify(notificationService)
                .markAsRead(7L, 10L);
    }

    // FR-92: Khong cho doc thong bao khi chua dang nhap
    @Test
    void shouldRejectMarkAsReadWithoutLogin() {

        when(accessControl.authenticate(null))
                .thenReturn(Optional.empty());

        ResponseEntity<?> response =
                controller.markNotificationAsRead(7L, null);

        assertEquals(401, response.getStatusCode().value());

        verifyNoInteractions(notificationService);
    }

    // FR-92: Khong cho tai khoan bi khoa danh dau da doc
    @Test
    void shouldRejectMarkAsReadForInactiveUser() {

        user.setActive(false);

        when(accessControl.authenticate("Bearer valid"))
                .thenReturn(Optional.of(user));

        ResponseEntity<?> response =
                controller.markNotificationAsRead(
                        7L, "Bearer valid"
                );

        assertEquals(403, response.getStatusCode().value());

        verifyNoInteractions(notificationService);
    }

    // FR-92: Tu choi ma thong bao khong hop le
    @Test
    void shouldRejectInvalidNotificationId() {

        when(accessControl.authenticate("Bearer valid"))
                .thenReturn(Optional.of(user));

        ResponseEntity<?> response =
                controller.markNotificationAsRead(
                        0L, "Bearer valid"
                );

        assertEquals(400, response.getStatusCode().value());

        verifyNoInteractions(notificationService);
    }

    // FR-92: Khong tiet lo thong bao khong thuoc tai khoan
    @Test
    void shouldNotExposeOtherUsersNotification() {

        when(accessControl.authenticate("Bearer valid"))
                .thenReturn(Optional.of(user));

        when(notificationService.markAsRead(99L, 10L))
                .thenReturn(Optional.empty());

        ResponseEntity<?> response =
                controller.markNotificationAsRead(
                        99L, "Bearer valid"
                );

        assertEquals(404, response.getStatusCode().value());

        verify(notificationService)
                .markAsRead(99L, 10L);
    }

    private CustomerNotification createNotification() {

        CustomerNotification notification =
                new CustomerNotification();

        notification.setUserId(10L);
        notification.setBookingId("HB-123456");
        notification.setType("PAYMENT_SUCCESS");
        notification.setTitle("Thanh toan thanh cong");
        notification.setMessage(
                "Don HB-123456 da thanh toan thanh cong."
        );

        return notification;
    }
}
