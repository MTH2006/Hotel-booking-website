package com.hotelbooking.controller;

import com.hotelbooking.model.User;
import com.hotelbooking.security.AccessControlService;
import com.hotelbooking.service.StatisticsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/statistics")
@CrossOrigin(origins = "*")
public class StatisticsController {

    private final StatisticsService statisticsService;
    private final AccessControlService accessControl;

    public StatisticsController(StatisticsService statisticsService, AccessControlService accessControl) {
        this.statisticsService = statisticsService;
        this.accessControl = accessControl;
    }

    private boolean checkAdminOrManager(String authorization) {
        Optional<User> user = accessControl.authenticate(authorization);
        if (user.isEmpty()) return false;
        String role = user.get().getRole();
        return "ADMIN".equalsIgnoreCase(role) || "MANAGER".equalsIgnoreCase(role);
    }

    // FR-85 -> FR-90: Tổng quan báo cáo thống kê
    @GetMapping
    public ResponseEntity<?> getStatistics(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        if (!checkAdminOrManager(authorization)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Chỉ quản trị viên hoặc quản lý mới có quyền xem thống kê."));
        }
        return ResponseEntity.ok(statisticsService.getComprehensiveReport());
    }

    // FR-85: Thống kê doanh thu
    @GetMapping("/revenue")
    public ResponseEntity<?> getRevenue(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        if (!checkAdminOrManager(authorization)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(statisticsService.getRevenueStatistics());
    }

    // FR-86: Thống kê đơn theo trạng thái
    @GetMapping("/bookings-by-status")
    public ResponseEntity<?> getBookingsByStatus(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        if (!checkAdminOrManager(authorization)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(statisticsService.getBookingsByStatus());
    }

    // FR-87: Tỷ lệ lấp đầy phòng
    @GetMapping("/occupancy-rate")
    public ResponseEntity<?> getOccupancyRate(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        if (!checkAdminOrManager(authorization)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(statisticsService.getOccupancyRate());
    }

    // FR-88: Top khách sạn doanh thu cao
    @GetMapping("/top-hotels")
    public ResponseEntity<?> getTopHotels(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        if (!checkAdminOrManager(authorization)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(statisticsService.getTopRevenueHotels());
    }

    // FR-89: Top loại phòng được đặt nhiều
    @GetMapping("/top-room-types")
    public ResponseEntity<?> getTopRoomTypes(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        if (!checkAdminOrManager(authorization)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(statisticsService.getTopBookedRoomTypes());
    }
}

