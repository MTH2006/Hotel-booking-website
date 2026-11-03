
package com.hotelbooking.controller;

import com.hotelbooking.model.SupportRequest;
import com.hotelbooking.service.SupportRequestService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/support/requests")
@CrossOrigin(origins = "*")
public class SupportRequestController {

    private final SupportRequestService supportRequestService;

    public SupportRequestController(
            SupportRequestService supportRequestService) {
        this.supportRequestService = supportRequestService;
    }

    // FR-94: Khach hang gui yeu cau ho tro
    @PostMapping
    public ResponseEntity<Map<String, Object>> createSupportRequest(
            @RequestBody SupportRequest request) {

        try {

            // Kiem tra thong tin va luu yeu cau vao PostgreSQL
            SupportRequest saved =
                    supportRequestService.createSupportRequest(request);

            // Chi tra ve thong tin can thiet, khong lo email/noi dung
            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "Gui yeu cau ho tro thanh cong.",
                    "requestId", saved.getId(),
                    "status", saved.getStatus()
            );

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(response);

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));
        }
    }
}
