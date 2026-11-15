
package com.hotelbooking.controller;

import com.hotelbooking.model.Faq;
import com.hotelbooking.model.User;
import com.hotelbooking.security.AccessControlService;
import com.hotelbooking.service.FaqService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

@RestController
@RequestMapping("/api/faqs")
@CrossOrigin(origins = {
        "http://localhost:3000",
        "http://localhost:5173"
})
public class FaqController {

    private final FaqService faqService;
    private final AccessControlService accessControl;

    public FaqController(
            FaqService faqService,
            AccessControlService accessControl) {

        this.faqService = faqService;
        this.accessControl = accessControl;
    }

    // FR-96: Khach hang xem danh sach FAQ
    @GetMapping
    public ResponseEntity<List<Faq>> getAllFaqs() {

        return ResponseEntity.ok(
                faqService.getAllFaqs()
        );
    }

    // FR-96: Chi ADMIN duoc them FAQ
    @PostMapping
    public ResponseEntity<?> createFaq(
            @RequestBody(required = false) Faq request,
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorization) {

        Optional<User> currentUser =
                accessControl.authenticate(authorization);

        if (currentUser.isEmpty()) {
            return ResponseEntity.status(401).build();
        }

        if (!accessControl.isAdmin(currentUser.get())) {
            return ResponseEntity.status(403).build();
        }

        try {
            Faq created = faqService.createFaq(request);

            return ResponseEntity.status(201).body(created);

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message", e.getMessage()
                    )
            );
        }
    }

    // FR-96: Chi ADMIN duoc chinh sua FAQ
    @PutMapping("/{id}")
    public ResponseEntity<?> updateFaq(
            @PathVariable Long id,
            @RequestBody(required = false) Faq request,
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorization) {

        Optional<User> currentUser =
                accessControl.authenticate(authorization);

        if (currentUser.isEmpty()) {
            return ResponseEntity.status(401).build();
        }

        if (!accessControl.isAdmin(currentUser.get())) {
            return ResponseEntity.status(403).build();
        }

        try {
            Faq updated = faqService.updateFaq(id, request);

            return ResponseEntity.ok(updated);

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message", e.getMessage()
                    )
            );

        } catch (NoSuchElementException e) {

            return ResponseEntity.status(404).body(
                    Map.of(
                            "success", false,
                            "message", e.getMessage()
                    )
            );
        }
    }

    // FR-96: Chi ADMIN duoc xoa FAQ
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFaq(
            @PathVariable Long id,
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorization) {

        Optional<User> currentUser =
                accessControl.authenticate(authorization);

        if (currentUser.isEmpty()) {
            return ResponseEntity.status(401).build();
        }

        if (!accessControl.isAdmin(currentUser.get())) {
            return ResponseEntity.status(403).build();
        }

        try {
            faqService.deleteFaq(id);

            return ResponseEntity.noContent().build();

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message", e.getMessage()
                    )
            );

        } catch (NoSuchElementException e) {

            return ResponseEntity.status(404).body(
                    Map.of(
                            "success", false,
                            "message", e.getMessage()
                    )
            );
        }
    }
}
