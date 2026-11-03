package com.hotelbooking.controller;

import com.hotelbooking.model.SupportRequest;
import com.hotelbooking.model.User;
import com.hotelbooking.security.AccessControlService;
import com.hotelbooking.service.SupportRequestService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/support/requests")
@CrossOrigin(origins = "*")
public class SupportRequestManagementController {

    private final SupportRequestService supportRequestService;
    private final AccessControlService accessControl;

    public SupportRequestManagementController(
            SupportRequestService supportRequestService,
            AccessControlService accessControl) {

        this.supportRequestService = supportRequestService;
        this.accessControl = accessControl;
    }

    // FR-95: Nhan vien xem danh sach yeu cau ho tro
    @GetMapping
    public ResponseEntity<List<SupportRequest>> getAllSupportRequests(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorization) {

        Optional<User> currentUser = accessControl.authenticate(authorization);
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(401).build();
        }

        if (!accessControl.isManagerOrAdmin(currentUser.get())) {
            return ResponseEntity.status(403).build();
        }

        List<SupportRequest> requests = supportRequestService.getAllSupportRequests();
        return ResponseEntity.ok(requests);
    }
    
    // FR-95: ADMIN hoac MANAGER tiep nhan yeu cau ho tro
    @PutMapping("/{requestId}/accept")
    public ResponseEntity<?> acceptSupportRequest(
            @PathVariable Long requestId,
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorization) {

        Optional<User> currentUser = accessControl.authenticate(authorization);
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(401).build();
        }

        if (!accessControl.isManagerOrAdmin(currentUser.get())) {
            return ResponseEntity.status(403).build();
        }

        try {
            SupportRequest updated = supportRequestService.acceptSupportRequest(requestId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Tiep nhan yeu cau ho tro thanh cong.",
                    "requestId", updated.getId(),
                    "status", updated.getStatus()
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // FR-96: Nhan vien phan hoi va dong yeu cau ho tro
    @PutMapping("/{requestId}/resolve")
    public ResponseEntity<?> resolveSupportRequest(
            @PathVariable Long requestId,
            @RequestBody(required = false) Map<String, String> body,
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorization) {

        Optional<User> currentUser = accessControl.authenticate(authorization);
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(401).build();
        }

        if (!accessControl.isManagerOrAdmin(currentUser.get())) {
            return ResponseEntity.status(403).build();
        }

        String responseMessage = body != null ? body.get("responseMessage") : "Yêu cầu đã được giải quyết.";
        try {
            SupportRequest updated = supportRequestService.resolveSupportRequest(requestId, responseMessage);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã phản hồi và đóng yêu cầu hỗ trợ thành công.",
                    "requestId", updated.getId(),
                    "status", updated.getStatus(),
                    "responseMessage", updated.getResponseMessage()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
