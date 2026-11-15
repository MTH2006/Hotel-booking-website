
package com.hotelbooking.controller;

import com.hotelbooking.model.CustomerNotification;
import com.hotelbooking.model.User;
import com.hotelbooking.security.AccessControlService;
import com.hotelbooking.service.CustomerNotificationService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = {
        "http://localhost:3000",
        "http://localhost:5173"
})
public class CustomerNotificationController {

    private final CustomerNotificationService notificationService;
    private final AccessControlService accessControl;

    public CustomerNotificationController(
            CustomerNotificationService notificationService,
            AccessControlService accessControl) {

        this.notificationService = notificationService;
        this.accessControl = accessControl;
    }

    // FR-92: Lay thong bao cua tai khoan dang dang nhap
    @GetMapping("/my")
    public ResponseEntity<?> getMyNotifications(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorization) {

        Optional<User> currentUser =
                accessControl.authenticate(authorization);

        // Token khong hop le hoac chua dang nhap
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(401).body(
                    Map.of(
                            "success", false,
                            "message", "Vui long dang nhap."
                    )
            );
        }

        // Tai khoan bi khoa khong duoc truy cap
        if (!currentUser.get().isActive()) {
            return ResponseEntity.status(403).body(
                    Map.of(
                            "success", false,
                            "message", "Tai khoan da bi khoa."
                    )
            );
        }

        // Chi lay thong bao cua nguoi dung da xac thuc
        Long userId = currentUser.get().getId();

        List<CustomerNotification> notifications =
                notificationService.getNotifications(userId);

        return ResponseEntity.ok(notifications);
    }

    // FR-92: Danh dau mot thong bao da doc
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<?> markNotificationAsRead(
            @PathVariable Long notificationId,
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorization) {

        Optional<User> currentUser =
                accessControl.authenticate(authorization);

        if (currentUser.isEmpty()) {
            return ResponseEntity.status(401).body(
                    Map.of(
                            "success", false,
                            "message", "Vui long dang nhap."
                    )
            );
        }

        if (!currentUser.get().isActive()) {
            return ResponseEntity.status(403).body(
                    Map.of(
                            "success", false,
                            "message", "Tai khoan da bi khoa."
                    )
            );
        }

        if (notificationId == null || notificationId <= 0) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message", "Ma thong bao khong hop le."
                    )
            );
        }

        // Lay userId tu JWT, khong lay tu request cua khach
        Long userId = currentUser.get().getId();

        Optional<CustomerNotification> updated =
                notificationService.markAsRead(
                        notificationId,
                        userId
                );

        if (updated.isEmpty()) {
            // Khong tiet lo thong bao cua tai khoan khac
            return ResponseEntity.status(404).body(
                    Map.of(
                            "success", false,
                            "message", "Khong tim thay thong bao."
                    )
            );
        }

        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "message", "Da danh dau thong bao da doc.",
                        "notification", updated.get()
                )
        );
    }
}
