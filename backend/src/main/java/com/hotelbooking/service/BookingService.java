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
import java.util.Map;
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
    private final Random random = new Random();

    // Optional để không phá các constructor đang dùng trong unit test
    @Autowired(required = false)
    private PromotionService promotionService;

    
    // FR-93: Luu thong bao thay doi va huy dat phong
    @Autowired(required = false)
    private BookingChangeNotificationPersistenceService
            bookingChangePersistenceService;
    
@Autowired(required = false)
private BookingConfirmationNotificationPersistenceService
        bookingConfirmationPersistenceService;

    @Autowired
    public BookingService(
            BookingRepository bookingRepository,
            HotelRepository hotelRepository,
            RoomRepository roomRepository,
            Optional<BookingNotificationService> notificationService,
            Optional<BookingChangeNotificationService> changeNotificationService) {
        this.bookingRepository = bookingRepository;
        this.hotelRepository = hotelRepository;
        this.roomRepository = roomRepository;
        this.notificationService = notificationService.orElse(null);
        this.changeNotificationService = changeNotificationService.orElse(null);
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
            Optional<Hotel> hotelOpt = hotelRepository.findById(booking.getHotelId());
            if (hotelOpt.isPresent()) {
                booking.setHotelName(hotelOpt.get().getName());
            }
        }

        // 3. Xác thực phòng & sức chứa [FR-25]
        Room room = null;
        if (booking.getRoomId() != null) {
            room = roomRepository.findById(booking.getRoomId()).orElse(null);
        }
        if (room == null && booking.getHotelId() != null && booking.getRoomType() != null) {
            room = roomRepository.findByHotelIdAndRoomType(booking.getHotelId(), booking.getRoomType()).orElse(null);
        }

        if (room != null) {
            booking.setRoom(room);
            int guestsCount = (booking.getAdults() != null ? booking.getAdults() : 2) +
                    (booking.getChildren() != null ? booking.getChildren() : 0);
            if (room.getCapacity() != null && guestsCount > room.getCapacity()) {
                throw new IllegalArgumentException("Số lượng khách (" + guestsCount
                        + ") vượt quá sức chứa tối đa của phòng (" + room.getCapacity() + " người).");
            }
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

        if (overlappingCount >= maxAvailableRooms) {
            throw new IllegalStateException(
                    "Hạng phòng '" + (booking.getRoomType() != null ? booking.getRoomType() : "đã chọn")
                            + "' đã hết phòng trong khoảng thời gian đã chọn!");
        }

        // 5. Tính toán tổng chi phí [FR-27]
        long nights = ChronoUnit.DAYS.between(booking.getCheckIn(), booking.getCheckOut());
        if (nights <= 0)
            nights = 1;

        if (booking.getTotalPrice() == null || booking.getTotalPrice() <= 0) {
            long pricePerNight = (room != null && room.getPricePerNight() != null) ? room.getPricePerNight() : 1500000L;
            long calculatedPrice = pricePerNight * nights;
            if (booking.getDiscountAmount() != null && booking.getDiscountAmount() > 0) {
                calculatedPrice = Math.max(0, calculatedPrice - booking.getDiscountAmount());
            }
            booking.setTotalPrice(calculatedPrice);
        }

        // 5b. Kiểm tra lại mã khuyến mãi ở phía server & ghi nhận lượt sử dụng [FR-53, FR-54]
        String promoCode = booking.getPromotionCode();
        if (promotionService != null && promoCode != null && !promoCode.isBlank()) {
            long clientDiscount = booking.getDiscountAmount() != null ? booking.getDiscountAmount() : 0L;
            long baseAmount = (booking.getTotalPrice() != null ? booking.getTotalPrice() : 0L) + clientDiscount;
            Map<String, Object> promoResult = promotionService.applyPromotion(promoCode, baseAmount);
            if (!Boolean.TRUE.equals(promoResult.get("valid"))) {
                throw new IllegalArgumentException(String.valueOf(promoResult.get("message")));
            }
            long discount = ((Number) promoResult.get("discountAmount")).longValue();
            booking.setPromotionCode(String.valueOf(promoResult.get("code")));
            booking.setDiscountAmount(discount);
            booking.setTotalPrice(Math.max(0L, baseAmount - discount));
            promotionService.incrementUsedCount(booking.getPromotionCode());
        }

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
        log.info("Booking created successfully: ID={}, Code={}", saved.getId(), saved.getBookingId());

        // 8. FR-91: Luu thong bao khi dat phong thanh cong
        if (bookingConfirmationPersistenceService != null) {
            bookingConfirmationPersistenceService
                    .saveBookingConfirmationNotification(saved);

            log.info(
                    "FR-91: Saved booking confirmation notification for {}",
                    saved.getBookingId()
            );
        } else {
            log.warn(
                    "FR-91: Notification persistence service is unavailable for {}",
                    saved.getBookingId()
            );
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

            // FR-93: Luu thong bao huy don dat phong
            if (bookingChangePersistenceService != null) {
                bookingChangePersistenceService
                        .saveBookingCancellationNotification(b);

                log.info(
                        "FR-93: Saved cancellation notification for {}",
                        b.getBookingId()
                );
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

        
        // FR-93: Luu thong bao huy don theo ID
        if (bookingChangePersistenceService != null) {
            bookingChangePersistenceService
                    .saveBookingCancellationNotification(saved);

            log.info(
                    "FR-93: Saved cancellation notification for {}",
                    saved.getBookingId()
            );
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

    
    // FR-93: Cap nhat trang thai va luu thong bao
    @Transactional
    public Booking updateStatus(Long id, String newStatus, String reason) {

        Booking booking = getBookingById(id);

        // Luu trang thai cu truoc khi thay doi
        Booking before = new Booking();
        before.setBookingId(booking.getBookingId());
        before.setStatus(booking.getStatus());
        before.setCheckIn(booking.getCheckIn());
        before.setCheckOut(booking.getCheckOut());
        before.setRoomType(booking.getRoomType());
        before.setTotalPrice(booking.getTotalPrice());

        // Cap nhat trang thai moi
        booking.setStatus(newStatus);

        if (reason != null && !reason.isBlank()) {
            booking.setCancellationReason(reason);
        }

        Booking saved = bookingRepository.save(booking);

        // Chi tao thong bao khi trang thai thay doi
        if (bookingChangePersistenceService != null
                && !java.util.Objects.equals(
                        before.getStatus(),
                        saved.getStatus())) {

            if ("CANCELLED".equalsIgnoreCase(saved.getStatus())) {
                bookingChangePersistenceService
                        .saveBookingCancellationNotification(saved);
            } else {
                bookingChangePersistenceService
                        .saveBookingUpdateNotification(before, saved);
            }

            log.info(
                    "FR-93: Saved status change notification for {}",
                    saved.getBookingId()
            );
        }

        return saved;
    }

    
    // FR-93: Cap nhat trang thai theo bookingId va luu thong bao
    @Transactional
    public Booking updateStatusByBookingId(
            String bookingId,
            String newStatus,
            String reason) {

        Booking booking = getBookingById(bookingId);

        if (booking == null) {
            throw new RuntimeException(
                    "Không tìm thấy đơn đặt phòng: " + bookingId
            );
        }

        // Luu thong tin truoc khi thay doi
        Booking before = new Booking();
        before.setBookingId(booking.getBookingId());
        before.setStatus(booking.getStatus());
        before.setCheckIn(booking.getCheckIn());
        before.setCheckOut(booking.getCheckOut());
        before.setRoomType(booking.getRoomType());
        before.setTotalPrice(booking.getTotalPrice());

        // Cap nhat trang thai moi
        booking.setStatus(newStatus);

        if (reason != null && !reason.isBlank()) {
            booking.setCancellationReason(reason);
        }

        Booking saved = bookingRepository.save(booking);

        // Chi luu thong bao khi trang thai thuc su thay doi
        if (bookingChangePersistenceService != null
                && !java.util.Objects.equals(
                        before.getStatus(),
                        saved.getStatus())) {

            if ("CANCELLED".equalsIgnoreCase(saved.getStatus())) {

                bookingChangePersistenceService
                        .saveBookingCancellationNotification(saved);

            } else {

                bookingChangePersistenceService
                        .saveBookingUpdateNotification(before, saved);
            }

            log.info(
                    "FR-93: Saved status change notification for {}",
                    saved.getBookingId()
            );
        }

        return saved;
    }


    public List<Booking> searchStaffBookings(String keyword, Long hotelId, String status) {
        return bookingRepository.searchStaffBookings(keyword, hotelId, status);
    }
}
private void validateTransition(String currentStatus, String newStatus) {
        if (currentStatus == null || newStatus == null) return;
        if (Booking.BookingStatus.COMPLETED.equals(currentStatus) && !Booking.BookingStatus.COMPLETED.equals(newStatus)) {
            throw new IllegalStateException("Đơn đã hoàn tất (COMPLETED) không thể chuyển sang trạng thái khác.");
        }
        if (Booking.BookingStatus.CANCELLED.equals(currentStatus) && !Booking.BookingStatus.CANCELLED.equals(newStatus)) {
            throw new IllegalStateException("Đơn đã hủy (CANCELLED) không thể khôi phục trực tiếp.");
        }
        if (Booking.BookingStatus.CHECKED_IN.equals(currentStatus)
                && Booking.BookingStatus.PENDING.equals(newStatus)) {
            throw new IllegalStateException("Đơn đã nhận phòng (CHECKED_IN) không thể quay về PENDING.");
        }
    }
