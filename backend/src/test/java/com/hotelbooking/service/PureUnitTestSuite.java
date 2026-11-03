package com.hotelbooking.service;

import com.hotelbooking.dto.AuthRequest;
import com.hotelbooking.dto.AuthResponse;
import com.hotelbooking.model.Booking;
import com.hotelbooking.model.Hotel;
import com.hotelbooking.model.Promotion;
import com.hotelbooking.model.Room;
import com.hotelbooking.model.SupportRequest;
import com.hotelbooking.model.User;
import com.hotelbooking.repository.BookingRepository;
import com.hotelbooking.repository.HotelRepository;
import com.hotelbooking.repository.PromotionRepository;
import com.hotelbooking.repository.RoomRepository;
import com.hotelbooking.repository.SupportRequestRepository;
import com.hotelbooking.repository.UserRepository;
import com.hotelbooking.security.AuthTokenService;
import com.hotelbooking.security.PasswordService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class PureUnitTestSuite {

    @SuppressWarnings("unchecked")
    private static <T> T createProxy(Class<T> iface, java.util.function.BiFunction<String, Object[], Object> handler) {
        return (T) Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[]{iface}, (proxy, method, args) -> {
            String name = method.getName();
            return handler.apply(name, args);
        });
    }

    // FR-1 & FR-3: Kiểm thử Đăng ký và Đăng nhập
    @Test
    void testAuthServiceRegisterAndLogin() {
        Map<String, User> db = new HashMap<>();
        UserRepository userRepo = createProxy(UserRepository.class, (method, args) -> {
            if ("existsByEmail".equals(method)) return db.containsKey(args[0]);
            if ("findByEmail".equals(method)) return Optional.ofNullable(db.get(args[0]));
            if ("findByUserUuid".equals(method)) {
                return db.values().stream().filter(u -> args[0].equals(u.getUserUuid())).findFirst();
            }
            if ("save".equals(method)) {
                User u = (User) args[0];
                u.setId(10L);
                db.put(u.getEmail(), u);
                return u;
            }
            return null;
        });

        AuthTokenService tokenService = new AuthTokenService(
                "test_secret_for_unit_tests_1234567890",
                new com.fasterxml.jackson.databind.ObjectMapper(),
                userRepo
        );
        PasswordService passwordService = new PasswordService();
        AuthService authService = new AuthService(userRepo, tokenService, passwordService);

        // 1. Đăng ký thành công
        AuthRequest reg = new AuthRequest();
        reg.setName("Test User");
        reg.setEmail("test@gmail.com");
        reg.setPassword("Password123");

        AuthResponse regResp = authService.register(reg);
        assertTrue(regResp.isSuccess());
        assertNotNull(regResp.getToken());
        assertEquals("test@gmail.com", regResp.getUser().getEmail());

        // 2. Đăng ký trùng email thất bại
        AuthResponse dupResp = authService.register(reg);
        assertFalse(dupResp.isSuccess());

        // 3. Đăng nhập đúng mật khẩu
        AuthRequest login = new AuthRequest();
        login.setEmail("test@gmail.com");
        login.setPassword("Password123");
        AuthResponse loginResp = authService.login(login);
        assertTrue(loginResp.isSuccess());
        assertNotNull(loginResp.getToken());

        // 4. Đăng nhập sai mật khẩu thất bại
        login.setPassword("WrongPassword");
        AuthResponse failResp = authService.login(login);
        assertFalse(failResp.isSuccess());
    }

    // FR-21 -> FR-29: Kiểm thử Tạo đơn đặt phòng, Tính giá, Chống Overbooking
    @Test
    void testBookingServiceWorkflow() {
        Map<String, Booking> bookingDb = new HashMap<>();
        BookingRepository bookingRepo = createProxy(BookingRepository.class, (method, args) -> {
            if ("countOverlappingBookings".equals(method)) return 1L; // 1 đơn đã đặt
            if ("findByBookingId".equals(method)) return Optional.ofNullable(bookingDb.get(args[0]));
            if ("save".equals(method)) {
                Booking b = (Booking) args[0];
                b.setId(1L);
                bookingDb.put(b.getBookingId(), b);
                return b;
            }
            return null;
        });

        Hotel hotel = new Hotel();
        hotel.setId(1L);
        hotel.setName("Grand Vista Hotel");
        HotelRepository hotelRepo = createProxy(HotelRepository.class, (method, args) -> Optional.of(hotel));

        Room room = new Room(10L, 1L, "Deluxe King", 1850000L, 2, 5, "Nice");
        RoomRepository roomRepo = createProxy(RoomRepository.class, (method, args) -> Optional.of(room));

        BookingNotificationService notif = new BookingNotificationService();
        BookingChangeNotificationService changeNotif = new BookingChangeNotificationService();
        BookingService bookingService = new BookingService(bookingRepo, hotelRepo, roomRepo, Optional.of(notif), Optional.of(changeNotif));

        Booking req = new Booking();
        req.setHotelId(1L);
        req.setRoomType("Deluxe King");
        req.setCheckIn(LocalDate.now().plusDays(2));
        req.setCheckOut(LocalDate.now().plusDays(5)); // 3 nights
        req.setGuestName("Nguyen Van An");
        req.setGuestEmail("an@example.com");
        req.setGuestPhone("0901234567");

        Booking created = bookingService.createBooking(req);
        assertNotNull(created);
        assertEquals("Grand Vista Hotel", created.getHotelName());
        assertEquals(5550000L, created.getTotalPrice()); // 3 nights * 1850000
        assertEquals(Booking.BookingStatus.CONFIRMED, created.getStatus());
        assertTrue(created.getBookingId().startsWith("HB-"));

        // Hủy đặt phòng
        boolean cancelled = bookingService.cancelBooking(created.getBookingId());
        assertTrue(cancelled);
        assertEquals(Booking.BookingStatus.CANCELLED, created.getStatus());
    }

    // FR-51 -> FR-53: Kiểm thử Khuyến mãi
    @Test
    void testPromotionService() {
        Promotion promo = new Promotion("HESTORA2026", "Giam 10%", 10.0, 500000L, 1000000L,
                LocalDate.now().minusDays(5), LocalDate.now().plusDays(30), 100);

        PromotionRepository promoRepo = createProxy(PromotionRepository.class, (method, args) -> {
            if ("findByCodeIgnoreCase".equals(method)) {
                String c = (String) args[0];
                return "HESTORA2026".equalsIgnoreCase(c) ? Optional.of(promo) : Optional.empty();
            }
            return null;
        });

        PromotionService service = new PromotionService(promoRepo);

        // Áp dụng hợp lệ
        Map<String, Object> res = service.applyPromotion("HESTORA2026", 2000000L);
        assertTrue((Boolean) res.get("valid"));
        assertEquals(200000L, res.get("discountAmount"));
        assertEquals(1800000L, res.get("finalAmount"));

        // Áp dụng mã không tồn tại
        Map<String, Object> resInvalid = service.applyPromotion("UNKNOWN", 2000000L);
        assertFalse((Boolean) resInvalid.get("valid"));
    }

    // FR-91: Kiểm thử Thông báo xác nhận đặt phòng
    @Test
    void testBookingNotificationService() {
        BookingNotificationService service = new BookingNotificationService();
        Booking b = new Booking();
        b.setBookingId("HB-999888");
        b.setGuestName("Tran Van B");
        b.setHotelName("Melia Nha Trang");
        b.setRoomType("Ocean Suite");
        b.setCheckIn(LocalDate.of(2026, 12, 1));
        b.setCheckOut(LocalDate.of(2026, 12, 4));
        b.setTotalPrice(8400000L);

        String notif = service.buildBookingConfirmation(b);
        assertNotNull(notif);
        assertTrue(notif.contains("HB-999888"));
        assertTrue(notif.contains("Tran Van B"));
        assertTrue(notif.contains("8400000"));
    }

    // FR-92: Kiểm thử Thông báo thanh toán
    @Test
    void testPaymentNotificationService() {
        PaymentNotificationService service = new PaymentNotificationService();
        String success = service.buildPaymentSuccessNotification("HB-999888", 8400000L);
        assertTrue(success.contains("Thanh toan thanh cong"));
        assertTrue(success.contains("HB-999888"));

        String fail = service.buildPaymentFailureNotification("HB-999888");
        assertTrue(fail.contains("Thanh toan that bai"));
    }

    // FR-93: Kiểm thử Thông báo thay đổi / hủy đơn
    @Test
    void testBookingChangeNotificationService() {
        BookingChangeNotificationService service = new BookingChangeNotificationService();
        Booking b = new Booking();
        b.setBookingId("HB-999888");
        b.setGuestName("Tran Van B");
        b.setHotelName("Melia Nha Trang");
        b.setStatus("CANCELLED");

        String notif = service.buildCancellationNotification(b);
        assertTrue(notif.contains("HB-999888"));
        assertTrue(notif.contains("CANCELLED"));
    }

    // FR-94 & FR-95 & FR-96: Kiểm thử Yêu cầu hỗ trợ khách hàng
    @Test
    void testSupportRequestService() {
        Map<Long, SupportRequest> db = new HashMap<>();
        SupportRequestRepository repo = createProxy(SupportRequestRepository.class, (method, args) -> {
            if ("save".equals(method)) {
                SupportRequest r = (SupportRequest) args[0];
                if (r.getId() == null) r.setId(100L);
                db.put(r.getId(), r);
                return r;
            }
            if ("findById".equals(method)) {
                return Optional.ofNullable(db.get(args[0]));
            }
            if ("acceptIfPending".equals(method)) {
                SupportRequest r = db.get(args[0]);
                if (r != null && "PENDING".equals(r.getStatus())) {
                    r.setStatus("ACCEPTED");
                    return 1;
                }
                return 0;
            }
            return null;
        });

        SupportRequestService service = new SupportRequestService(repo);

        SupportRequest req = new SupportRequest();
        req.setCustomerName("Nguyen Van A");
        req.setCustomerEmail("a@example.com");
        req.setSubject("Can tu van dat phong");
        req.setMessage("Toi can ho tro dat phong gia dinh.");

        SupportRequest created = service.createSupportRequest(req);
        assertNotNull(created);
        assertEquals("PENDING", created.getStatus());

        SupportRequest accepted = service.acceptSupportRequest(created.getId());
        assertEquals("ACCEPTED", accepted.getStatus());

        SupportRequest resolved = service.resolveSupportRequest(created.getId(), "Da goi dien tu van xong cho khach.");
        assertEquals("RESOLVED", resolved.getStatus());
        assertEquals("Da goi dien tu van xong cho khach.", resolved.getResponseMessage());
    }

    // FR-26 & FR-31: Kiểm thử Ngăn chặn Overbooking khi hết phòng khả dụng
    @Test
    void testOverbookingRejection() {
        BookingRepository bookingRepo = createProxy(BookingRepository.class, (method, args) -> {
            if ("countOverlappingBookings".equals(method)) return 5L; // Đã kín 5/5 phòng
            return null;
        });

        Hotel hotel = new Hotel();
        hotel.setId(1L);
        hotel.setName("Grand Vista Hotel");
        HotelRepository hotelRepo = createProxy(HotelRepository.class, (method, args) -> Optional.of(hotel));

        Room room = new Room(10L, 1L, "Standard", 1500000L, 2, 5, "Nice"); // 5 phòng tổng cộng
        RoomRepository roomRepo = createProxy(RoomRepository.class, (method, args) -> Optional.of(room));

        BookingService bookingService = new BookingService(bookingRepo, hotelRepo, roomRepo, Optional.empty(), Optional.empty());

        Booking req = new Booking();
        req.setHotelId(1L);
        req.setRoomType("Standard");
        req.setCheckIn(LocalDate.now().plusDays(1));
        req.setCheckOut(LocalDate.now().plusDays(3));
        req.setGuestName("Khach Thu Sau");

        assertThrows(IllegalStateException.class, () -> bookingService.createBooking(req));
    }

    // FR-25: Kiểm thử Ràng buộc sức chứa phòng
    @Test
    void testOverCapacityRejection() {
        BookingRepository bookingRepo = createProxy(BookingRepository.class, (method, args) -> 0L);
        HotelRepository hotelRepo = createProxy(HotelRepository.class, (method, args) -> Optional.of(new Hotel()));
        Room room = new Room(10L, 1L, "Single Room", 1000000L, 1, 10, "1 Guest max"); // Sức chứa 1 người
        RoomRepository roomRepo = createProxy(RoomRepository.class, (method, args) -> Optional.of(room));

        BookingService bookingService = new BookingService(bookingRepo, hotelRepo, roomRepo, Optional.empty(), Optional.empty());

        Booking req = new Booking();
        req.setHotelId(1L);
        req.setRoomType("Single Room");
        req.setCheckIn(LocalDate.now().plusDays(1));
        req.setCheckOut(LocalDate.now().plusDays(2));
        req.setAdults(3); // 3 người lớn > sức chứa 1

        assertThrows(IllegalArgumentException.class, () -> bookingService.createBooking(req));
    }

    // FR-38 -> FR-44: Kiểm thử Xử lý Thanh toán, Hoàn tiền và Biên lai
    @Test
    void testPaymentServiceWorkflow() {
        Map<String, com.hotelbooking.model.Payment> paymentDb = new HashMap<>();
        com.hotelbooking.repository.PaymentRepository paymentRepo = createProxy(com.hotelbooking.repository.PaymentRepository.class, (method, args) -> {
            if ("save".equals(method)) {
                com.hotelbooking.model.Payment p = (com.hotelbooking.model.Payment) args[0];
                p.setId(99L);
                paymentDb.put(p.getTransactionId(), p);
                return p;
            }
            if ("findByTransactionId".equals(method)) {
                return Optional.ofNullable(paymentDb.get(args[0]));
            }
            if ("findByBookingIdOrderByCreatedAtDesc".equals(method)) {
                return new ArrayList<>(paymentDb.values());
            }
            return null;
        });

        Booking b = new Booking();
        b.setBookingId("HB-555666");
        b.setGuestEmail("guest@gmail.com");
        b.setTotalPrice(3500000L);
        BookingRepository bookingRepo = createProxy(BookingRepository.class, (method, args) -> {
            if ("findByBookingId".equals(method)) return Optional.of(b);
            if ("save".equals(method)) return args[0];
            return null;
        });

        PaymentNotificationService notif = new PaymentNotificationService();
        PaymentService paymentService = new PaymentService(paymentRepo, bookingRepo, Optional.of(notif));

        // 1. Thực hiện thanh toán VNPay thành công
        com.hotelbooking.model.Payment p = paymentService.processPayment("HB-555666", "VNPAY", 3500000L, "VNPAY-QR-APP");
        assertNotNull(p);
        assertEquals("SUCCESS", p.getStatus());
        assertEquals("PAID", b.getPaymentStatus());
        assertNotNull(p.getTransactionId());

        // 2. Xuất biên lai điện tử
        Map<String, Object> receipt = paymentService.generateReceipt(p.getTransactionId());
        assertNotNull(receipt);
        assertEquals(3500000L, receipt.get("amount"));
        assertEquals("VNPAY", receipt.get("paymentMethod"));

        // 3. Hoàn tiền khi hủy đơn
        com.hotelbooking.model.Payment refunded = paymentService.processRefund(p.getTransactionId(), "Khach huy phong truoc 24h");
        assertEquals("REFUNDED", refunded.getStatus());
        assertNotNull(refunded.getRefundedAt());
    }
}
