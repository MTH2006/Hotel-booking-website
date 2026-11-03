package com.hotelbooking.controller;

import com.hotelbooking.model.Payment;
import com.hotelbooking.model.User;
import com.hotelbooking.security.AccessControlService;
import com.hotelbooking.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
public class PaymentController {

    private final PaymentService paymentService;
    private final AccessControlService accessControl;

    public PaymentController(PaymentService paymentService, AccessControlService accessControl) {
        this.paymentService = paymentService;
        this.accessControl = accessControl;
    }

    // FR-38, 39, 40, 41: Khởi tạo và xử lý thanh toán
    @PostMapping("/process")
    public ResponseEntity<?> processPayment(@RequestBody Map<String, Object> payload) {
        String bookingId = (String) payload.get("bookingId");
        String paymentMethod = (String) payload.get("paymentMethod");
        Number amountNum = (Number) payload.get("amount");
        Long amount = amountNum != null ? amountNum.longValue() : null;
        String cardInfo = (String) payload.get("cardInfo");

        try {
            Payment payment = paymentService.processPayment(bookingId, paymentMethod, amount, cardInfo);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Thanh toán thành công!",
                    "payment", payment
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Lỗi xử lý thanh toán: " + e.getMessage()));
        }
    }

    // FR-42: Lấy lịch sử giao dịch của 1 đơn đặt phòng
    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<List<Payment>> getPaymentsByBooking(@PathVariable String bookingId) {
        return ResponseEntity.ok(paymentService.getPaymentsByBookingId(bookingId));
    }

    // FR-43: Xử lý hoàn tiền
    @PostMapping("/{transactionId}/refund")
    public ResponseEntity<?> refundPayment(
            @PathVariable String transactionId,
            @RequestBody(required = false) Map<String, String> body,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Optional<User> user = accessControl.authenticate(authorization);
        if (user.isEmpty() || (!accessControl.isAdmin(user.get()) && !"STAFF".equalsIgnoreCase(user.get().getRole()))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Chỉ nhân viên hoặc quản trị viên mới có quyền hoàn tiền."));
        }

        String reason = body != null ? body.get("reason") : "Hoàn tiền cho khách hàng";
        try {
            Payment refunded = paymentService.processRefund(transactionId, reason);
            return ResponseEntity.ok(Map.of("success", true, "message", "Đã xử lý hoàn tiền thành công!", "payment", refunded));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // FR-44: Lấy biên lai điện tử
    @GetMapping("/receipt/{transactionId}")
    public ResponseEntity<?> getReceipt(@PathVariable String transactionId) {
        try {
            Map<String, Object> receipt = paymentService.generateReceipt(transactionId);
            return ResponseEntity.ok(receipt);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        }
    }
}

