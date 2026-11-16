package com.hotelbooking.controller;

import com.hotelbooking.model.Booking;
import com.hotelbooking.model.Review;
import com.hotelbooking.repository.BookingRepository;
import com.hotelbooking.repository.UserRepository;
import com.hotelbooking.service.AuthSessionService;
import com.hotelbooking.service.ReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@CrossOrigin(origins = "*")
public class ReviewController {
    private final ReviewService service;
    private final AuthSessionService sessions;
    private final UserRepository users;
    private final BookingRepository bookings;

    public ReviewController(ReviewService service, AuthSessionService sessions, UserRepository users, BookingRepository bookings) {
        this.service = service; this.sessions = sessions; this.users = users; this.bookings = bookings;
    }

    @GetMapping
    public List<Review> published(@RequestParam Long hotelId) { return service.published(hotelId); }

    @GetMapping("/mine")
    public ResponseEntity<?> mine(@RequestHeader(value = "Authorization", required = false) String auth) {
        String uuid = sessions.userId(auth); return uuid == null ? unauthorized() : ResponseEntity.ok(service.mine(uuid));
    }

    @GetMapping("/eligible-bookings")
    public ResponseEntity<?> eligible(@RequestHeader(value = "Authorization", required = false) String auth) {
        String uuid = sessions.userId(auth); if (uuid == null) return unauthorized();
        var user = users.findByUserUuid(uuid).orElse(null); if (user == null) return unauthorized();
        List<Map<String, Object>> eligible = bookings.findByGuestEmailIgnoreCaseAndStatusAndCheckOutLessThanEqualOrderByCheckOutDesc(user.getEmail(), "COMPLETED", LocalDate.now())
                .stream().map(b -> Map.<String, Object>of("id", b.getId(), "bookingId", b.getBookingId(), "hotelId", b.getHotelId(), "hotelName", b.getHotelName(), "checkOut", b.getCheckOut())).toList();
        return ResponseEntity.ok(eligible);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestHeader(value = "Authorization", required = false) String auth, @RequestBody ReviewRequest request) {
        String uuid = sessions.userId(auth); if (uuid == null) return unauthorized();
        try { return ResponseEntity.ok(service.create(uuid, request.bookingId(), request.rating(), request.comment())); }
        catch (IllegalArgumentException e) { return ResponseEntity.badRequest().body(Map.of("message", e.getMessage())); }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> edit(@RequestHeader(value = "Authorization", required = false) String auth, @PathVariable Long id, @RequestBody ReviewRequest request) {
        String uuid = sessions.userId(auth); if (uuid == null) return unauthorized();
        try { return ResponseEntity.ok(service.edit(uuid, id, request.rating(), request.comment())); }
        catch (IllegalArgumentException e) { return ResponseEntity.badRequest().body(Map.of("message", e.getMessage())); }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@RequestHeader(value = "Authorization", required = false) String auth, @PathVariable Long id) {
        String uuid = sessions.userId(auth); if (uuid == null) return unauthorized();
        try { service.delete(uuid, id); return ResponseEntity.noContent().build(); }
        catch (IllegalArgumentException e) { return ResponseEntity.badRequest().body(Map.of("message", e.getMessage())); }
    }

    @GetMapping("/admin/pending")
    public ResponseEntity<?> pending(@RequestHeader(value = "Authorization", required = false) String auth) {
        return sessions.isAdmin(auth) ? ResponseEntity.ok(service.pending()) : ResponseEntity.status(403).body(Map.of("message", "Chỉ quản trị viên mới có quyền kiểm duyệt."));
    }

    @PutMapping("/admin/{id}/moderation")
    public ResponseEntity<?> moderate(@RequestHeader(value = "Authorization", required = false) String auth, @PathVariable Long id, @RequestBody Map<String, String> body) {
        if (!sessions.isAdmin(auth)) return ResponseEntity.status(403).body(Map.of("message", "Chỉ quản trị viên mới có quyền kiểm duyệt."));
        try { return ResponseEntity.ok(service.moderate(id, body.get("status"))); }
        catch (IllegalArgumentException e) { return ResponseEntity.badRequest().body(Map.of("message", e.getMessage())); }
    }

    private ResponseEntity<Map<String, String>> unauthorized() { return ResponseEntity.status(401).body(Map.of("message", "Vui lòng đăng nhập lại.")); }
    public record ReviewRequest(Long bookingId, Integer rating, String comment) {}
}
