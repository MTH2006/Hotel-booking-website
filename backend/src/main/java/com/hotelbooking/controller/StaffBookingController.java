package com.hotelbooking.controller;

import com.hotelbooking.model.Booking;
import com.hotelbooking.model.User;
import com.hotelbooking.security.AccessControlService;
import com.hotelbooking.service.BookingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/staff/bookings")
@CrossOrigin(origins = "*")
public class StaffBookingController {

    private final BookingService bookingService;
    private final AccessControlService accessControl;

    public StaffBookingController(BookingService bookingService, AccessControlService accessControl) {
        this.bookingService = bookingService;
        this.accessControl = accessControl;
    }

    private boolean isStaffOrAdmin(String authorization) {
        Optional<User> user = accessControl.authenticate(authorization);
        if (user.isEmpty()) return false;
        String role = user.get().getRole();
        return "ADMIN".equalsIgnoreCase(role) || "STAFF".equalsIgnoreCase(role) || "MANAGER".equalsIgnoreCase(role);
    }

    // FR-71: Xem danh sách đơn đặt phòng
    @GetMapping
    public ResponseEntity<?> getStaffBookings(
            @RequestParam(required = false) Long hotelId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        if (!isStaffOrAdmin(authorization)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Chỉ nhân viên hoặc quản trị viên mới có quyền truy cập."));
        }
        List<Booking> bookings = (hotelId != null) 
                ? bookingService.searchStaffBookings(null, hotelId, null)
                : bookingService.getAllBookings();
        return ResponseEntity.ok(Map.of("status", "success", "data", bookings));
    }

    // FR-72: Tìm kiếm đơn đặt phòng theo mã đơn, khách hàng, khách sạn hoặc trạng thái
    @GetMapping("/search")
    public ResponseEntity<?> searchStaffBookings(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long hotelId,
            @RequestParam(required = false) String status,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        if (!isStaffOrAdmin(authorization)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Chỉ nhân viên hoặc quản trị viên mới có quyền truy cập."));
        }
        List<Booking> bookings = bookingService.searchStaffBookings(keyword, hotelId, status);
        return ResponseEntity.ok(Map.of("status", "success", "data", bookings));
    }

    // FR-73: Xác nhận đơn đặt phòng theo quy trình nghiệp vụ
    @PatchMapping("/{id}/confirm")
    public ResponseEntity<?> confirmBooking(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        if (!isStaffOrAdmin(authorization)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Từ chối truy cập."));
        }
        try {
            Booking b = updateBookingStatus(id, Booking.BookingStatus.CONFIRMED, null);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Đã xác nhận đơn đặt phòng thành công!", "booking", b));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // FR-74: Từ chối yêu cầu đặt phòng (ghi nhận lý do)
    @PatchMapping("/{id}/reject")
    public ResponseEntity<?> rejectBooking(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        if (!isStaffOrAdmin(authorization)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Từ chối truy cập."));
        }
        String reason = body != null ? body.get("reason") : "Nhân viên từ chối";
        try {
            Booking b = updateBookingStatus(id, Booking.BookingStatus.CANCELLED, reason);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Đã từ chối đơn đặt phòng.", "reason", reason, "booking", b));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // FR-75: Cập nhật trạng thái đơn theo tiến trình xử lý thực tế
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable String id,
            @RequestBody Map<String, String> body,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        if (!isStaffOrAdmin(authorization)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Từ chối truy cập."));
        }
        String status = body != null ? body.get("status") : null;
        String reason = body != null ? body.get("reason") : null;
        if (status == null || status.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Trạng thái không được để trống."));
        }
        try {
            Booking b = updateBookingStatus(id, status.toUpperCase(), reason);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Cập nhật trạng thái thành công!", "booking", b));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // FR-76: Xử lý yêu cầu hủy phòng theo chính sách
    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelBooking(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        if (!isStaffOrAdmin(authorization)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Từ chối truy cập."));
        }
        String reason = body != null ? body.get("reason") : "Xử lý hủy phòng theo chính sách";
        try {
            Booking b = updateBookingStatus(id, Booking.BookingStatus.CANCELLED, reason);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Đã xử lý yêu cầu hủy phòng!", "booking", b));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // FR-77: Quản lý nhận phòng (Check-in)
    @PatchMapping("/{id}/check-in")
    public ResponseEntity<?> checkIn(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        if (!isStaffOrAdmin(authorization)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Từ chối truy cập."));
        }
        try {
            Booking b = updateBookingStatus(id, Booking.BookingStatus.CHECKED_IN, "Khách hàng đã làm thủ tục nhận phòng.");
            return ResponseEntity.ok(Map.of("status", "success", "message", "Check-in thành công!", "booking", b));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // FR-78: Quản lý trả phòng (Check-out)
    @PatchMapping("/{id}/check-out")
    public ResponseEntity<?> checkOut(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        if (!isStaffOrAdmin(authorization)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Từ chối truy cập."));
        }
        try {
            Booking b = updateBookingStatus(id, Booking.BookingStatus.COMPLETED, "Khách hàng đã làm thủ tục trả phòng.");
            return ResponseEntity.ok(Map.of("status", "success", "message", "Check-out thành công!", "booking", b));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    private Booking updateBookingStatus(String id, String status, String reason) {
        try {
            Long numericId = Long.parseLong(id);
            return bookingService.updateStatus(numericId, status, reason);
        } catch (NumberFormatException e) {
            return bookingService.updateStatusByBookingId(id, status, reason);
        }
    }
}

