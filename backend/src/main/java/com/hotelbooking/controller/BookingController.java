package com.hotelbooking.controller;

import com.hotelbooking.model.Booking;
import com.hotelbooking.model.User;
import com.hotelbooking.security.AccessControlService;
import com.hotelbooking.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/bookings")
@CrossOrigin(origins = "*")
public class BookingController {

    private final BookingService bookingService;
    private final AccessControlService accessControl;

    @Autowired
    public BookingController(BookingService bookingService, AccessControlService accessControl) {
        this.bookingService = bookingService;
        this.accessControl = accessControl;
    }

    // FR-21 -> FR-29: Đặt phòng mới
    @PostMapping
    public ResponseEntity<Map<String, Object>> createBooking(
            @RequestBody Booking booking,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Tự động gán userId nếu có token
            if (authorization != null && !authorization.isBlank()) {
                Optional<User> userOpt = accessControl.authenticate(authorization);
                userOpt.ifPresent(u -> {
                    if (booking.getUserId() == null) booking.setUserId(u.getId());
                    if (booking.getGuestName() == null || booking.getGuestName().isBlank()) booking.setGuestName(u.getName());
                    if (booking.getGuestEmail() == null || booking.getGuestEmail().isBlank()) booking.setGuestEmail(u.getEmail());
                    if (booking.getGuestPhone() == null || booking.getGuestPhone().isBlank()) booking.setGuestPhone(u.getPhone());
                });
            }

            Booking created = bookingService.createBooking(booking);
            response.put("success", true);
            response.put("booking", created);
            response.put("message", "Đặt phòng thành công! Mã đơn của bạn là: " + created.getBookingId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi xử lý đặt phòng: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // FR-30: Lịch sử đặt phòng theo userId
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Booking>> getUserBookings(@PathVariable Long userId) {
        return ResponseEntity.ok(bookingService.getBookingsByUserId(userId));
    }

    // FR-30: Lịch sử đặt phòng của tài khoản đang đăng nhập
    @GetMapping("/my")
    public ResponseEntity<List<Booking>> getMyBookings(@RequestHeader(value = "Authorization", required = false) String authorization) {
        Optional<User> userOpt = accessControl.authenticate(authorization);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(bookingService.getBookingsByUserId(userOpt.get().getId()));
    }

    // FR-71: Xem tất cả đơn (Admin / Staff)
    @GetMapping
    public ResponseEntity<List<Booking>> getAllBookings(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return ResponseEntity.ok(bookingService.getAllBookings());
    }

    // FR-31 & FR-33: Lấy chi tiết đơn đặt phòng theo ID hoặc bookingId
    @GetMapping("/{identifier}")
    public ResponseEntity<Booking> getBookingByIdentifier(@PathVariable String identifier) {
        try {
            Long numericId = Long.parseLong(identifier);
            Booking b = bookingService.getBookingById(numericId);
            return ResponseEntity.ok(b);
        } catch (NumberFormatException e) {
            Booking b = bookingService.getBookingById(identifier);
            if (b == null) return ResponseEntity.notFound().build();
            return ResponseEntity.ok(b);
        }
    }

    // FR-34: Hủy đơn đặt phòng theo id hoặc bookingId
    @PutMapping("/{identifier}/cancel")
    public ResponseEntity<Map<String, Object>> cancelBooking(
            @PathVariable String identifier,
            @RequestBody(required = false) Map<String, String> body,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean success = false;
            String reason = body != null ? body.get("reason") : "Khách hàng yêu cầu hủy";
            try {
                Long numericId = Long.parseLong(identifier);
                Booking b = bookingService.cancelBooking(numericId);
                success = (b != null);
            } catch (NumberFormatException ex) {
                success = bookingService.cancelBooking(identifier);
            }

            if (success) {
                response.put("success", true);
                response.put("message", "Đã hủy đơn đặt phòng thành công.");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Không tìm thấy đơn đặt phòng.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (IllegalStateException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // FR-46 & Quản trị: Hoàn tất đơn đặt phòng sau ngày trả phòng
    @PutMapping("/{bookingId}/complete")
    public ResponseEntity<Map<String, Object>> completeBooking(
            @PathVariable String bookingId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Optional<User> user = accessControl.authenticate(authorization);
        if (user.isEmpty() || !accessControl.isAdmin(user.get())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "message", "Chỉ quản trị viên mới có quyền hoàn tất đơn."));
        }
        boolean completed = bookingService.completeBooking(bookingId);
        return completed ? ResponseEntity.ok(Map.of("success", true, "message", "Đơn lưu trú đã hoàn tất."))
                : ResponseEntity.badRequest().body(Map.of("success", false, "message", "Chỉ có thể hoàn tất đơn đã xác nhận sau ngày trả phòng."));
    }

    // FR-37: Xuất dữ liệu biên lai / voucher
    @GetMapping("/{identifier}/voucher")
    public ResponseEntity<Map<String, Object>> getBookingVoucher(@PathVariable String identifier) {
        Booking booking = null;
        try {
            Long numericId = Long.parseLong(identifier);
            booking = bookingService.getBookingById(numericId);
        } catch (NumberFormatException e) {
            booking = bookingService.getBookingById(identifier);
        }

        if (booking == null) return ResponseEntity.notFound().build();

        Map<String, Object> voucher = new HashMap<>();
        voucher.put("voucherNumber", "VCH-" + booking.getBookingId());
        voucher.put("bookingId", booking.getBookingId());
        voucher.put("guestName", booking.getGuestName());
        voucher.put("hotelName", booking.getHotelName());
        voucher.put("roomType", booking.getRoomType());
        voucher.put("checkIn", booking.getCheckIn());
        voucher.put("checkOut", booking.getCheckOut());
        voucher.put("totalPrice", booking.getTotalPrice());
        voucher.put("paymentStatus", booking.getPaymentStatus());
        voucher.put("status", booking.getStatus());
        voucher.put("terms", "Quý khách vui lòng xuất trình CMND/CCCD và mã voucher này khi làm thủ tục check-in tại quầy lễ tân.");

        return ResponseEntity.ok(voucher);
    }
}
