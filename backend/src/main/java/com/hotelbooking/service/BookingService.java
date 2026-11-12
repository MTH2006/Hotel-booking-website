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
    private final Random random = new Random();

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
