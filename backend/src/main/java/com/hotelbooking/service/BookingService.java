package com.hotelbooking.service;

import com.hotelbooking.model.Booking;
import com.hotelbooking.model.Hotel;
import com.hotelbooking.model.Room;
import com.hotelbooking.repository.BookingRepository;
import com.hotelbooking.repository.HotelRepository;
import com.hotelbooking.repository.RoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository bookingRepository;
    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final BookingNotificationService notificationService;
    private final BookingChangeNotificationService changeNotificationService;
    private final PromotionService promotionService;
    private final Random random = new Random();

    @Autowired
    public BookingService(
            BookingRepository bookingRepository,
            HotelRepository hotelRepository,
            RoomRepository roomRepository,
            Optional<BookingNotificationService> notificationService,
            Optional<BookingChangeNotificationService> changeNotificationService,
            Optional<PromotionService> promotionService) {
        this.bookingRepository = bookingRepository;
        this.hotelRepository = hotelRepository;
        this.roomRepository = roomRepository;
        this.notificationService = notificationService.orElse(null);
        this.changeNotificationService = changeNotificationService.orElse(null);
        this.promotionService = promotionService.orElse(null);
    }

    // Constructor giữ tương thích với các unit test / nơi khởi tạo service cũ.
    public BookingService(BookingRepository bookingRepository, HotelRepository hotelRepository,
            RoomRepository roomRepository, Optional<BookingNotificationService> notificationService,
            Optional<BookingChangeNotificationService> changeNotificationService) {
        this(bookingRepository, hotelRepository, roomRepository, notificationService, changeNotificationService, Optional.empty());
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Booking createBooking(Booking booking) {
        // 1. Kiểm tra ngày nhận & trả phòng [FR-24]
        if (booking.getCheckIn() == null || booking.getCheckOut() == null) {
            throw new IllegalArgumentException("Ngày nhận phòng và ngày trả phòng không được để trống.");
        }
        if (!booking.getCheckOut().isAfter(booking.getCheckIn())) {
            throw new IllegalArgumentException("Ngày trả phòng phải sau ngày nhận phòng.");
        }
        if (booking.getCheckIn().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Ngày nhận phòng không thể ở quá khứ.");
        }

        // 2. Xác thực khách sạn tồn tại
        if (booking.getHotelId() != null) {
            hotelRepository.findById(booking.getHotelId()).ifPresent(hotel -> booking.setHotelName(hotel.getName()));
        }

        // 3. Xác thực phòng & sức chứa [FR-25]
        Room room = null;
        if (booking.getRoomId() != null) {
            room = roomRepository.findById(booking.getRoomId()).orElse(null);
        }
        if (room == null && booking.getHotelId() != null && booking.getRoomType() != null) {
            room = roomRepository.findByHotelIdAndRoomType(booking.getHotelId(), booking.getRoomType()).orElse(null);
        }

        if (room == null) {
            throw new IllegalArgumentException("Hạng phòng đã chọn không tồn tại.");
        }
        if (room.getHotelId() == null || !room.getHotelId().equals(booking.getHotelId())) {
            throw new IllegalArgumentException("Hạng phòng không thuộc khách sạn đã chọn.");
        }
        if (!Boolean.TRUE.equals(room.getIsActive()) || !"AVAILABLE".equalsIgnoreCase(room.getStatus())) {
            throw new IllegalStateException("Hạng phòng hiện không nhận đặt phòng.");
        }
        booking.setRoom(room);
        if (booking.getRoomType() == null) booking.setRoomType(room.getRoomType());
        int quantity = booking.getQuantity() == null ? 1 : booking.getQuantity();
        if (quantity <= 0) throw new IllegalArgumentException("Số lượng phòng phải lớn hơn 0.");
        booking.setQuantity(quantity);
        int adults = booking.getAdults() == null ? 2 : booking.getAdults();
        int children = booking.getChildren() == null ? 0 : booking.getChildren();
        if (adults <= 0 || children < 0) throw new IllegalArgumentException("Số lượng người lớn/trẻ em không hợp lệ.");
        int guestsCount = adults + children;
        if (room.getCapacity() != null && guestsCount > room.getCapacity() * quantity) {
            throw new IllegalArgumentException("Số lượng khách vượt quá sức chứa của số phòng đã chọn.");
        }

        // 4. Kiểm tra phòng khả dụng & chống overbooking [FR-26]
        int maxAvailableRooms = 10;
        if (room != null && room.getTotalRooms() != null) {
            maxAvailableRooms = room.getTotalRooms();
        }

        long overlappingCount = 0;
        if (booking.getHotelId() != null && booking.getRoomType() != null) {
            overlappingCount = bookingRepository.countOverlappingBookings(
                    booking.getHotelId(),
                    booking.getRoomType(),
                    booking.getCheckIn(),
                    booking.getCheckOut());
        }

        if (overlappingCount + quantity > maxAvailableRooms) {
            throw new IllegalStateException(
                    "Hạng phòng '" + booking.getRoomType()
                            + "' không đủ số lượng phòng trong khoảng thời gian đã chọn. Còn tối đa "
                            + Math.max(0, maxAvailableRooms - overlappingCount) + " phòng.");
        }

        // 5. Tính toán tổng chi phí [FR-27]
        long nights = ChronoUnit.DAYS.between(booking.getCheckIn(), booking.getCheckOut());
        if (nights <= 0)
            nights = 1;

        long pricePerNight = room.getPricePerNight();
        long baseAmount = Math.multiplyExact(Math.multiplyExact(pricePerNight, nights), quantity);
        long discountAmount = 0L;
        String promotionCode = booking.getPromotionCode();
        if (promotionCode != null && !promotionCode.isBlank()) {
            if (promotionService == null) {
                throw new IllegalStateException("Chức năng khuyến mãi hiện chưa khả dụng.");
            }
            java.util.Map<String, Object> promotionResult = promotionService.applyPromotion(promotionCode, baseAmount);
            if (!Boolean.TRUE.equals(promotionResult.get("valid"))) {
                throw new IllegalArgumentException(String.valueOf(promotionResult.get("message")));
            }
            discountAmount = ((Number) promotionResult.get("discountAmount")).longValue();
            booking.setPromotionCode(String.valueOf(promotionResult.get("code")));
        } else {
            booking.setPromotionCode(null);
        }
        booking.setDiscountAmount(discountAmount);
        booking.setTotalPrice(Math.max(0L, baseAmount - discountAmount));

        // 6. Sinh mã đặt phòng duy nhất [FR-29]
        if (booking.getBookingId() == null || booking.getBookingId().isBlank()) {
            String bId = "HB-" + (100000 + random.nextInt(900000));
            while (bookingRepository.findByBookingId(bId).isPresent()) {
                bId = "HB-" + (100000 + random.nextInt(900000));
            }
            booking.setBookingId(bId);
        }

        if (booking.getStatus() == null || booking.getStatus().isBlank()) {
            booking.setStatus(Booking.BookingStatus.CONFIRMED);
        }

        // 7. Lưu vào PostgreSQL
        Booking saved = bookingRepository.save(booking);
        if (promotionCode != null && !promotionCode.isBlank() && promotionService != null) {
            promotionService.incrementUsedCount(promotionCode);
        }
        log.info("Booking created successfully: ID={}, Code={}", saved.getId(), saved.getBookingId());

        // 8. Kích hoạt thông báo xác nhận [FR-91]
        try {
            if (notificationService != null) {
                notificationService.buildBookingConfirmation(saved);
            }
        } catch (Exception e) {
            log.warn("Failed to generate booking confirmation notification: {}", e.getMessage());
        }

        return saved;
    }

    public List<Booking> getAllBookings() {
        return bookingRepository.findAllByOrderByCreatedAtDesc();
    }

    public Booking getBookingById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt phòng với ID: " + id));
    }

    public Booking getBookingById(String bookingId) {
        return bookingRepository.findByBookingId(bookingId).orElse(null);
    }

    public List<Booking> getBookingsByUserId(Long userId) {
        return bookingRepository.findByUserIdAndHiddenFromUserFalseOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public boolean hideCancelledBookingForUser(String bookingId, Long userId) {
        Optional<Booking> found = bookingRepository.findByBookingId(bookingId);
        if (found.isEmpty()) return false;

        Booking booking = found.get();
        if (!userId.equals(booking.getUserId())
                || !Booking.BookingStatus.CANCELLED.equals(booking.getStatus())) {
            return false;
        }

        booking.setHiddenFromUser(true);
        bookingRepository.save(booking);
        return true;
    }

    @Transactional
    public boolean cancelBooking(String bookingId) {
        Optional<Booking> opt = bookingRepository.findByBookingId(bookingId);
        if (opt.isPresent()) {
            Booking b = opt.get();
            if (Booking.BookingStatus.CANCELLED.equals(b.getStatus())) {
                throw new IllegalStateException("Đơn đặt phòng này đã bị hủy trước đó.");
            }
            b.setStatus(Booking.BookingStatus.CANCELLED);
            bookingRepository.save(b);

            try {
                if (changeNotificationService != null) {
                    changeNotificationService.buildCancellationNotification(b);
                }
            } catch (Exception e) {
                log.warn("Failed to generate cancel notification: {}", e.getMessage());
            }
            return true;
        }
        return false;
    }

    @Transactional
    public Booking cancelBooking(Long id) {
        Booking booking = getBookingById(id);
        if (Booking.BookingStatus.CANCELLED.equals(booking.getStatus())) {
            throw new IllegalStateException("Đơn đặt phòng này đã bị hủy trước đó.");
        }
        booking.setStatus(Booking.BookingStatus.CANCELLED);
        Booking saved = bookingRepository.save(booking);

        try {
            if (changeNotificationService != null) {
                changeNotificationService.buildCancellationNotification(saved);
            }
        } catch (Exception e) {
            log.warn("Failed to generate cancel notification: {}", e.getMessage());
        }
        return saved;
    }

    @Transactional
    public boolean completeBooking(String bookingId) {
        Optional<Booking> opt = bookingRepository.findByBookingId(bookingId);
        if (opt.isEmpty())
            return false;
        Booking booking = opt.get();
        if (!Booking.BookingStatus.CONFIRMED.equals(booking.getStatus())
                && !Booking.BookingStatus.CHECKED_IN.equals(booking.getStatus())) {
            return false;
        }
        booking.setStatus(Booking.BookingStatus.COMPLETED);
        bookingRepository.save(booking);
        return true;
    }

    @Transactional
    public Booking updateStatus(Long id, String newStatus, String reason) {
        Booking booking = getBookingById(id);
        booking.setStatus(newStatus);
        if (reason != null && !reason.isBlank()) {
            booking.setCancellationReason(reason);
        }
        return bookingRepository.save(booking);
    }

    @Transactional
    public Booking updateStatusByBookingId(String bookingId, String newStatus, String reason) {
        Booking booking = getBookingById(bookingId);
        if (booking == null) {
            throw new RuntimeException("Không tìm thấy đơn đặt phòng: " + bookingId);
        }
        booking.setStatus(newStatus);
        if (reason != null && !reason.isBlank()) {
            booking.setCancellationReason(reason);
        }
        return bookingRepository.save(booking);
    }

    public List<Booking> searchStaffBookings(String keyword, Long hotelId, String status) {
        return bookingRepository.searchStaffBookings(keyword, hotelId, status);
    }
}
