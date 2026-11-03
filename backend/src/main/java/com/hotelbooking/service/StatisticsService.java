package com.hotelbooking.service;

import com.hotelbooking.model.Booking;
import com.hotelbooking.model.Hotel;
import com.hotelbooking.model.Room;
import com.hotelbooking.repository.BookingRepository;
import com.hotelbooking.repository.HotelRepository;
import com.hotelbooking.repository.RoomRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StatisticsService {

    private final BookingRepository bookingRepository;
    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;

    public StatisticsService(
            BookingRepository bookingRepository,
            HotelRepository hotelRepository,
            RoomRepository roomRepository) {
        this.bookingRepository = bookingRepository;
        this.hotelRepository = hotelRepository;
        this.roomRepository = roomRepository;
    }

    // FR-85: Thống kê tổng doanh thu
    public Map<String, Object> getRevenueStatistics() {
        List<Booking> bookings = bookingRepository.findAll();
        long totalRevenue = bookings.stream()
                .filter(b -> !"CANCELLED".equals(b.getStatus()))
                .mapToLong(b -> b.getTotalPrice() != null ? b.getTotalPrice() : 0L)
                .sum();

        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysAgo = today.minusDays(30);

        long monthlyRevenue = bookings.stream()
                .filter(b -> !"CANCELLED".equals(b.getStatus()) && b.getCreatedAt() != null && b.getCreatedAt().toLocalDate().isAfter(thirtyDaysAgo))
                .mapToLong(b -> b.getTotalPrice() != null ? b.getTotalPrice() : 0L)
                .sum();

        Map<String, Object> res = new HashMap<>();
        res.put("totalRevenue", totalRevenue);
        res.put("monthlyRevenue", monthlyRevenue);
        res.put("currency", "VND");
        return res;
    }

    // FR-86: Thống kê đơn theo trạng thái
    public Map<String, Long> getBookingsByStatus() {
        List<Booking> bookings = bookingRepository.findAll();
        Map<String, Long> counts = new HashMap<>();
        counts.put("TOTAL", (long) bookings.size());
        counts.put("CONFIRMED", bookings.stream().filter(b -> "CONFIRMED".equals(b.getStatus())).count());
        counts.put("CHECKED_IN", bookings.stream().filter(b -> "CHECKED_IN".equals(b.getStatus())).count());
        counts.put("COMPLETED", bookings.stream().filter(b -> "COMPLETED".equals(b.getStatus())).count());
        counts.put("CANCELLED", bookings.stream().filter(b -> "CANCELLED".equals(b.getStatus())).count());
        counts.put("PENDING", bookings.stream().filter(b -> "PENDING".equals(b.getStatus())).count());
        return counts;
    }

    // FR-87: Thống kê tỷ lệ lấp đầy phòng (Occupancy rate)
    public Map<String, Object> getOccupancyRate() {
        List<Room> allRooms = roomRepository.findAll();
        long totalRoomsCount = allRooms.stream()
                .mapToLong(r -> r.getTotalRooms() != null ? r.getTotalRooms() : 10)
                .sum();

        LocalDate today = LocalDate.now();
        List<Booking> activeBookings = bookingRepository.findAll().stream()
                .filter(b -> !"CANCELLED".equals(b.getStatus()))
                .filter(b -> (b.getCheckIn() != null && !b.getCheckIn().isAfter(today)) && 
                             (b.getCheckOut() != null && b.getCheckOut().isAfter(today)))
                .toList();

        long occupiedRooms = activeBookings.size();
        double rate = (totalRoomsCount > 0) ? ((double) occupiedRooms / totalRoomsCount) * 100.0 : 0.0;

        Map<String, Object> res = new HashMap<>();
        res.put("totalRooms", totalRoomsCount);
        res.put("occupiedRooms", occupiedRooms);
        res.put("occupancyRatePercent", Math.round(rate * 10.0) / 10.0);
        return res;
    }

    // FR-88: Báo cáo top khách sạn có doanh thu cao nhất
    public List<Map<String, Object>> getTopRevenueHotels() {
        List<Booking> bookings = bookingRepository.findAll().stream()
                .filter(b -> !"CANCELLED".equals(b.getStatus()))
                .toList();

        Map<Long, Long> revenueByHotel = bookings.stream()
                .filter(b -> b.getHotelId() != null)
                .collect(Collectors.groupingBy(
                        Booking::getHotelId,
                        Collectors.summingLong(b -> b.getTotalPrice() != null ? b.getTotalPrice() : 0L)
                ));

        List<Hotel> hotels = hotelRepository.findAll();
        Map<Long, Hotel> hotelMap = hotels.stream().collect(Collectors.toMap(Hotel::getId, h -> h));

        List<Map<String, Object>> result = new ArrayList<>();
        revenueByHotel.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(5)
                .forEach(entry -> {
                    Map<String, Object> item = new HashMap<>();
                    Long hId = entry.getKey();
                    Hotel h = hotelMap.get(hId);
                    item.put("hotelId", hId);
                    item.put("hotelName", h != null ? h.getName() : "Khách sạn #" + hId);
                    item.put("city", h != null ? h.getCity() : "");
                    item.put("totalRevenue", entry.getValue());
                    result.add(item);
                });

        return result;
    }

    // FR-89: Báo cáo hạng phòng được đặt nhiều nhất
    public List<Map<String, Object>> getTopBookedRoomTypes() {
        List<Booking> bookings = bookingRepository.findAll().stream()
                .filter(b -> !"CANCELLED".equals(b.getStatus()) && b.getRoomType() != null)
                .toList();

        Map<String, Long> countByType = bookings.stream()
                .collect(Collectors.groupingBy(Booking::getRoomType, Collectors.counting()));

        List<Map<String, Object>> result = new ArrayList<>();
        countByType.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .forEach(entry -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("roomType", entry.getKey());
                    item.put("bookingCount", entry.getValue());
                    result.add(item);
                });

        return result;
    }

    // FR-90: Xuất báo cáo tổng hợp
    public Map<String, Object> getComprehensiveReport() {
        Map<String, Object> report = new HashMap<>();
        report.put("generatedAt", LocalDate.now());
        report.put("revenue", getRevenueStatistics());
        report.put("bookingCounts", getBookingsByStatus());
        report.put("occupancy", getOccupancyRate());
        report.put("topHotels", getTopRevenueHotels());
        report.put("topRoomTypes", getTopBookedRoomTypes());
        return report;
    }
}

