package com.hotelbooking.service;

import com.hotelbooking.model.*;
import com.hotelbooking.repository.BookingRepository;
import com.hotelbooking.repository.HotelRepository;
import com.hotelbooking.repository.RoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class HotelToolService {

    private static final Logger log = LoggerFactory.getLogger(HotelToolService.class);

    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final BookingService bookingService;
    private final SupportRequestService supportRequestService;

    public HotelToolService(
            HotelRepository hotelRepository,
            RoomRepository roomRepository,
            BookingRepository bookingRepository,
            BookingService bookingService,
            SupportRequestService supportRequestService) {
        this.hotelRepository = hotelRepository;
        this.roomRepository = roomRepository;
        this.bookingRepository = bookingRepository;
        this.bookingService = bookingService;
        this.supportRequestService = supportRequestService;
    }

    /**
     * Tìm kiếm phòng khả dụng theo địa điểm, số khách và khoảng ngày.
     */
    public Map<String, Object> searchAvailableRooms(String destination, Integer guests, LocalDate checkIn, LocalDate checkOut) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> matchedRooms = new ArrayList<>();

        int reqGuests = (guests != null && guests > 0) ? guests : 2;
        LocalDate in = (checkIn != null) ? checkIn : LocalDate.now().plusDays(1);
        LocalDate out = (checkOut != null && checkOut.isAfter(in)) ? checkOut : in.plusDays(1);
        long nights = Math.max(1, ChronoUnit.DAYS.between(in, out));

        List<Hotel> candidateHotels;
        if (destination != null && !destination.isBlank()) {
            candidateHotels = hotelRepository.searchHotels(destination.trim());
        } else {
            candidateHotels = hotelRepository.findAll();
        }

        for (Hotel hotel : candidateHotels) {
            if (Boolean.FALSE.equals(hotel.getActive())) continue;
            List<Room> rooms = roomRepository.findByHotelId(hotel.getId());
            for (Room room : rooms) {
                if (Boolean.FALSE.equals(room.getIsActive())) continue;
                if ("MAINTENANCE".equalsIgnoreCase(room.getStatus())) continue;
                if (room.getCapacity() != null && room.getCapacity() < reqGuests) continue;

                // Kiểm tra tình trạng overbooking trong khoảng ngày
                long booked = bookingRepository.countOverlappingBookings(hotel.getId(), room.getRoomType(), in, out);
                int totalAvailable = (room.getTotalRooms() != null ? room.getTotalRooms() : 10) - (int) booked;

                if (totalAvailable > 0) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("hotelId", hotel.getId());
                    item.put("hotelName", hotel.getName());
                    item.put("city", hotel.getCity());
                    item.put("roomId", room.getId());
                    item.put("roomType", room.getRoomType());
                    item.put("pricePerNight", room.getPricePerNight());
                    item.put("totalPrice", room.getPricePerNight() * nights);
                    item.put("capacity", room.getCapacity());
                    item.put("bedType", room.getBedType());
                    item.put("availableRooms", totalAvailable);
                    item.put("checkIn", in);
                    item.put("checkOut", out);
                    item.put("nights", nights);
                    matchedRooms.add(item);
                }
            }
        }

        result.put("queryDestination", destination);
        result.put("checkIn", in);
        result.put("checkOut", out);
        result.put("guests", reqGuests);
        result.put("nights", nights);
        result.put("foundRoomsCount", matchedRooms.size());
        result.put("rooms", matchedRooms);
        return result;
    }

    /**
     * Tra cứu giá của một hạng phòng cụ thể.
     */
    public Map<String, Object> queryRoomPrice(String roomTypeName, String hotelName, int nights) {
        Map<String, Object> result = new LinkedHashMap<>();
        int countNights = Math.max(1, nights);

        String cleanQuery = EmbeddingService.stripAccents(roomTypeName != null ? roomTypeName.toLowerCase() : "");
        List<Room> rooms = roomRepository.findAll();
        Room bestMatch = null;
        int maxMatchedTokens = 0;

        for (Room r : rooms) {
            String rName = EmbeddingService.stripAccents(r.getRoomType().toLowerCase());
            String[] rTokens = rName.split("\\s+");
            int matched = 0;
            for (String t : rTokens) {
                if (t.length() >= 3 && cleanQuery.contains(t)) {
                    matched++;
                }
            }
            if (matched > maxMatchedTokens && matched >= 1) {
                maxMatchedTokens = matched;
                bestMatch = r;
            }
        }

        if (bestMatch != null) {
            Hotel h = hotelRepository.findById(bestMatch.getHotelId()).orElse(null);
            result.put("found", true);
            result.put("hotelName", h != null ? h.getName() : "HESTORA Hotel");
            result.put("roomType", bestMatch.getRoomType());
            result.put("pricePerNight", bestMatch.getPricePerNight());
            result.put("nights", countNights);
            result.put("estimatedTotal", bestMatch.getPricePerNight() * countNights);
            result.put("capacity", bestMatch.getCapacity());
            result.put("amenities", bestMatch.getAmenities());
            return result;
        }

        result.put("found", false);
        result.put("message", "Không tìm thấy thông tin giá cho hạng phòng được yêu cầu.");
        return result;
    }

    /**
     * Tra cứu đơn đặt phòng an toàn của người dùng (chỉ trả về đơn của chính người dùng).
     */
    public Map<String, Object> lookupUserBookings(User authenticatedUser, String requestedBookingId) {
        Map<String, Object> result = new LinkedHashMap<>();

        if (authenticatedUser == null) {
            result.put("authenticated", false);
            result.put("message", "Bạn cần đăng nhập tài khoản trên website để tra cứu chi tiết đơn đặt phòng.");
            return result;
        }

        result.put("authenticated", true);

        if (requestedBookingId != null && !requestedBookingId.isBlank()) {
            Booking b = bookingService.getBookingById(requestedBookingId.trim());
            if (b != null && (authenticatedUser.getId().equals(b.getUserId()) || authenticatedUser.getEmail().equalsIgnoreCase(b.getGuestEmail()))) {
                result.put("found", true);
                result.put("booking", sanitizeBooking(b));
                return result;
            } else {
                result.put("found", false);
                result.put("message", "Không tìm thấy mã đơn đặt phòng '" + requestedBookingId + "' thuộc tài khoản của bạn.");
                return result;
            }
        }

        // Lấy danh sách tất cả các đơn của người dùng
        List<Booking> userBookings = bookingService.getBookingsByUserId(authenticatedUser.getId());
        List<Map<String, Object>> sanitizedList = userBookings.stream().map(this::sanitizeBooking).toList();
        result.put("found", !sanitizedList.isEmpty());
        result.put("totalBookings", sanitizedList.size());
        result.put("bookings", sanitizedList);
        return result;
    }

    /**
     * Hỗ trợ hủy đơn đặt phòng với yêu cầu xác nhận rõ ràng.
     */
    public Map<String, Object> cancelBookingSafely(User authenticatedUser, String bookingId, boolean confirmed) {
        Map<String, Object> result = new LinkedHashMap<>();

        if (authenticatedUser == null) {
            result.put("success", false);
            result.put("message", "Vui lòng đăng nhập tài khoản để thực hiện yêu cầu hủy phòng.");
            return result;
        }

        if (bookingId == null || bookingId.isBlank()) {
            result.put("success", false);
            result.put("message", "Vui lòng cung cấp mã đơn đặt phòng (ví dụ: HB-821934) bạn muốn hủy.");
            return result;
        }

        Booking b = bookingService.getBookingById(bookingId.trim());
        if (b == null || (!authenticatedUser.getId().equals(b.getUserId()) && !authenticatedUser.getEmail().equalsIgnoreCase(b.getGuestEmail()))) {
            result.put("success", false);
            result.put("message", "Không tìm thấy đơn đặt phòng hợp lệ hoặc bạn không có quyền hủy đơn này.");
            return result;
        }

        if (Booking.BookingStatus.CANCELLED.equals(b.getStatus())) {
            result.put("success", false);
            result.put("message", "Đơn đặt phòng " + bookingId + " đã ở trạng thái ĐÃ HỦY trước đó.");
            return result;
        }

        // Tính khoảng thời gian đến ngày nhận phòng
        long daysUntilCheckin = ChronoUnit.DAYS.between(LocalDate.now(), b.getCheckIn());
        String policyNotice;
        if (daysUntilCheckin >= 2) {
            policyNotice = "Đơn của bạn đủ điều kiện HỦY MIỄN PHÍ 100% (còn hơn 48 giờ trước ngày nhận phòng).";
        } else if (daysUntilCheckin >= 1) {
            policyNotice = "Đơn hủy trong vòng 24h-48h sẽ có thể bị tính phí đêm đầu tiên theo chính sách khách sạn.";
        } else {
            policyNotice = "Đơn hủy trong vòng 24h trước giờ nhận phòng không được hoàn tiền theo chính sách quy định.";
        }

        if (!confirmed) {
            result.put("success", false);
            result.put("needsConfirmation", true);
            result.put("bookingId", b.getBookingId());
            result.put("hotelName", b.getHotelName());
            result.put("checkIn", b.getCheckIn());
            result.put("policyNotice", policyNotice);
            result.put("message", "Bạn có chắc chắn muốn hủy đơn " + b.getBookingId() + " tại " + b.getHotelName() + " (Check-in: " + b.getCheckIn() + ") không? " + policyNotice + "\nVui lòng phản hồi 'Xác nhận hủy " + b.getBookingId() + "' để tiến hành.");
            return result;
        }

        boolean cancelled = bookingService.cancelBooking(b.getBookingId());
        result.put("success", cancelled);
        result.put("bookingId", b.getBookingId());
        result.put("message", cancelled ? "Đã hủy đơn đặt phòng " + b.getBookingId() + " thành công. " + policyNotice : "Không thể thực hiện hủy đơn lúc này.");
        return result;
    }

    private Map<String, Object> sanitizeBooking(Booking b) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("bookingId", b.getBookingId());
        map.put("hotelName", b.getHotelName());
        map.put("roomType", b.getRoomType());
        map.put("checkIn", b.getCheckIn());
        map.put("checkOut", b.getCheckOut());
        map.put("totalPrice", b.getTotalPrice());
        map.put("status", b.getStatus());
        map.put("paymentStatus", b.getPaymentStatus());
        map.put("guestName", b.getGuestName());
        return map;
    }
}

