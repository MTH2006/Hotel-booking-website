package com.hotelbooking.service;

import com.hotelbooking.model.Booking;
import com.hotelbooking.model.Payment;
import com.hotelbooking.repository.BookingRepository;
import com.hotelbooking.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final PaymentNotificationService notificationService;
    private final Random random = new Random();

    public PaymentService(
            PaymentRepository paymentRepository,
            BookingRepository bookingRepository,
            Optional<PaymentNotificationService> notificationService) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.notificationService = notificationService.orElse(null);
    }

    // FR-38 -> FR-41: Xử lý giao dịch thanh toán
    @Transactional
    public Payment processPayment(String bookingId, String paymentMethod, Long amount, String cardOrBankInfo) {
        Optional<Booking> bookingOpt = bookingRepository.findByBookingId(bookingId);
        if (bookingOpt.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy đơn đặt phòng với mã: " + bookingId);
        }

        Booking booking = bookingOpt.get();

        if (amount == null || amount <= 0) {
            amount = booking.getTotalPrice();
        }

        // Sinh mã giao dịch duy nhất [FR-42]
        String txnId = "TXN-" + System.currentTimeMillis() + "-" + (1000 + random.nextInt(9000));

        Payment payment = new Payment();
        payment.setTransactionId(txnId);
        payment.setBookingId(bookingId);
        payment.setAmount(amount);
        payment.setPaymentMethod(paymentMethod != null ? paymentMethod : "CASH_ON_ARRIVAL");
        payment.setStatus("SUCCESS");
        payment.setPaymentGateway("MOCK_GATEWAY_VN");
        payment.setNote("Thanh toán hoàn tất qua " + payment.getPaymentMethod());

        Payment saved = paymentRepository.save(payment);

        // Cập nhật trạng thái thanh toán trên Booking [FR-41]
        booking.setPaymentMethod(payment.getPaymentMethod());
        booking.setPaymentStatus("PAID");
        bookingRepository.save(booking);

        // Kích hoạt thông báo thanh toán [FR-92]
        try {
            if (notificationService != null) {
                notificationService.buildPaymentSuccessNotification(bookingId, amount);
            }
        } catch (Exception e) {
            log.warn("Failed to generate payment notification: {}", e.getMessage());
        }

        return saved;
    }

    // FR-42: Tra cứu lịch sử thanh toán theo đơn
    public List<Payment> getPaymentsByBookingId(String bookingId) {
        return paymentRepository.findByBookingIdOrderByCreatedAtDesc(bookingId);
    }

    // FR-43: Xử lý hoàn tiền khi hủy đơn hợp lệ
    @Transactional
    public Payment processRefund(String transactionId, String refundReason) {
        Optional<Payment> opt = paymentRepository.findByTransactionId(transactionId);
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy giao dịch: " + transactionId);
        }

        Payment payment = opt.get();
        if ("REFUNDED".equals(payment.getStatus())) {
            throw new IllegalStateException("Giao dịch này đã được hoàn tiền trước đó.");
        }

        payment.setStatus("REFUNDED");
        payment.setRefundedAt(LocalDateTime.now());
        payment.setNote("Đã hoàn tiền: " + (refundReason != null ? refundReason : "Khách hàng hủy đơn"));

        Payment saved = paymentRepository.save(payment);

        // Cập nhật booking
        bookingRepository.findByBookingId(payment.getBookingId()).ifPresent(b -> {
            b.setPaymentStatus("REFUNDED");
            bookingRepository.save(b);
        });

        return saved;
    }

    // FR-44: Xuất biên lai thanh toán điện tử
    public Map<String, Object> generateReceipt(String transactionId) {
        Optional<Payment> opt = paymentRepository.findByTransactionId(transactionId);
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy giao dịch: " + transactionId);
        }

        Payment payment = opt.get();
        Booking booking = bookingRepository.findByBookingId(payment.getBookingId()).orElse(null);

        Map<String, Object> receipt = new HashMap<>();
        receipt.put("receiptNumber", "REC-" + payment.getTransactionId());
        receipt.put("transactionId", payment.getTransactionId());
        receipt.put("bookingId", payment.getBookingId());
        receipt.put("amount", payment.getAmount());
        receipt.put("paymentMethod", payment.getPaymentMethod());
        receipt.put("status", payment.getStatus());
        receipt.put("paidAt", payment.getCreatedAt());
        if (booking != null) {
            receipt.put("guestName", booking.getGuestName());
            receipt.put("guestEmail", booking.getGuestEmail());
            receipt.put("hotelName", booking.getHotelName());
            receipt.put("roomType", booking.getRoomType());
            receipt.put("checkIn", booking.getCheckIn());
            receipt.put("checkOut", booking.getCheckOut());
        }

        return receipt;
    }
}

