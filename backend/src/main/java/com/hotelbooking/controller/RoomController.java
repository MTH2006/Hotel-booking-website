package com.hotelbooking.controller;

import com.hotelbooking.model.Room;
import com.hotelbooking.model.User;
import com.hotelbooking.repository.RoomRepository;
import com.hotelbooking.security.AccessControlService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class RoomController {

    private final RoomRepository roomRepository;
    private final AccessControlService accessControl;

    public RoomController(RoomRepository roomRepository, AccessControlService accessControl) {
        this.roomRepository = roomRepository;
        this.accessControl = accessControl;
    }

    // FR-63 & FR-17: Lấy danh sách hạng phòng theo khách sạn
    @GetMapping("/hotels/{hotelId}/rooms")
    public ResponseEntity<List<Room>> getRoomsByHotel(@PathVariable Long hotelId) {
        List<Room> rooms = roomRepository.findByHotelIdAndIsActiveTrue(hotelId);
        return ResponseEntity.ok(rooms);
    }

    // FR-18 & FR-64: Lấy chi tiết 1 hạng phòng
    @GetMapping("/rooms/{id}")
    public ResponseEntity<Room> getRoomById(@PathVariable Long id) {
        return roomRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // FR-64, FR-65, FR-66: Thêm mới hạng phòng (Admin / Manager)
    @PostMapping("/hotels/{hotelId}/rooms")
    public ResponseEntity<Room> createRoom(
            @PathVariable Long hotelId,
            @RequestBody Room room,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Optional<User> user = accessControl.authenticate(authorization);
        if (user.isEmpty() || (!accessControl.isAdmin(user.get()) && !"MANAGER".equalsIgnoreCase(user.get().getRole()))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String validationError = validateRoom(room, true);
        if (validationError != null) return ResponseEntity.badRequest().build();
        if (roomRepository.findByHotelIdAndRoomType(hotelId, room.getRoomType().trim()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        room.setId(null);
        room.setHotelId(hotelId);
        room.setRoomType(room.getRoomType().trim());
        if (room.getStatus() == null) room.setStatus("AVAILABLE");
        if (room.getIsActive() == null) room.setIsActive(true);
        Room saved = roomRepository.save(room);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // FR-65, FR-67, FR-68: Cập nhật thông tin hạng phòng
    @PutMapping({"/rooms/{id}", "/hotels/rooms/{id}"})
    public ResponseEntity<Room> updateRoom(
            @PathVariable Long id,
            @RequestBody Room update,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Optional<User> user = accessControl.authenticate(authorization);
        if (user.isEmpty() || (!accessControl.isAdmin(user.get()) && !"MANAGER".equalsIgnoreCase(user.get().getRole()))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Optional<Room> existingOpt = roomRepository.findById(id);
        if (existingOpt.isEmpty()) return ResponseEntity.notFound().build();

        String validationError = validateRoom(update, false);
        if (validationError != null) return ResponseEntity.badRequest().build();

        Room existing = existingOpt.get();
        if (update.getRoomType() != null) {
            String newType = update.getRoomType().trim();
            Optional<Room> duplicate = roomRepository.findByHotelIdAndRoomType(existing.getHotelId(), newType);
            if (duplicate.isPresent() && !duplicate.get().getId().equals(id)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
            existing.setRoomType(newType);
        }
        if (update.getPricePerNight() != null) existing.setPricePerNight(update.getPricePerNight());
        if (update.getCapacity() != null) existing.setCapacity(update.getCapacity());
        if (update.getTotalRooms() != null) existing.setTotalRooms(update.getTotalRooms());
        if (update.getAreaSqm() != null) existing.setAreaSqm(update.getAreaSqm());
        if (update.getBedType() != null) existing.setBedType(update.getBedType());
        if (update.getAmenities() != null) existing.setAmenities(update.getAmenities());
        if (update.getImageUrl() != null) existing.setImageUrl(update.getImageUrl());
        if (update.getStatus() != null) existing.setStatus(update.getStatus());
        if (update.getIsActive() != null) existing.setIsActive(update.getIsActive());
        if (update.getDescription() != null) existing.setDescription(update.getDescription());

        return ResponseEntity.ok(roomRepository.save(existing));
    }

    // FR-69: Cập nhật trạng thái hoạt động của phòng (AVAILABLE, MAINTENANCE)
    @PatchMapping("/rooms/{id}/status")
    public ResponseEntity<Room> updateRoomStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Optional<User> user = accessControl.authenticate(authorization);
        if (user.isEmpty() || (!accessControl.isAdmin(user.get()) && !"MANAGER".equalsIgnoreCase(user.get().getRole()))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Optional<Room> existingOpt = roomRepository.findById(id);
        if (existingOpt.isEmpty()) return ResponseEntity.notFound().build();

        String normalizedStatus = status == null ? "" : status.trim().toUpperCase();
        if (!normalizedStatus.equals("AVAILABLE") && !normalizedStatus.equals("MAINTENANCE")) {
            return ResponseEntity.badRequest().build();
        }
        Room existing = existingOpt.get();
        existing.setStatus(normalizedStatus);
        return ResponseEntity.ok(roomRepository.save(existing));
    }

    // FR-70: Xóa hoặc ngừng kinh doanh hạng phòng
    @DeleteMapping({"/rooms/{id}", "/hotels/rooms/{id}"})
    public ResponseEntity<Void> deleteRoom(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Optional<User> user = accessControl.authenticate(authorization);
        if (user.isEmpty() || (!accessControl.isAdmin(user.get()) && !"MANAGER".equalsIgnoreCase(user.get().getRole()))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Optional<Room> roomOpt = roomRepository.findById(id);
        if (roomOpt.isEmpty()) return ResponseEntity.notFound().build();
        Room room = roomOpt.get();
        room.setIsActive(false); // Ngừng kinh doanh, không xóa dữ liệu lịch sử đặt phòng.
        roomRepository.save(room);
        return ResponseEntity.noContent().build();
    }

    private String validateRoom(Room room, boolean creating) {
        if (creating && (room.getRoomType() == null || room.getRoomType().isBlank())) return "Tên hạng phòng không được để trống.";
        if (room.getRoomType() != null && room.getRoomType().isBlank()) return "Tên hạng phòng không hợp lệ.";
        if (room.getPricePerNight() != null && room.getPricePerNight() <= 0) return "Giá phòng phải lớn hơn 0.";
        if (creating && (room.getPricePerNight() == null || room.getPricePerNight() <= 0)) return "Giá phòng phải lớn hơn 0.";
        if (room.getCapacity() != null && room.getCapacity() <= 0) return "Sức chứa phải lớn hơn 0.";
        if (creating && (room.getCapacity() == null || room.getCapacity() <= 0)) return "Sức chứa phải lớn hơn 0.";
        if (room.getTotalRooms() != null && room.getTotalRooms() <= 0) return "Số lượng phòng phải lớn hơn 0.";
        if (creating && (room.getTotalRooms() == null || room.getTotalRooms() <= 0)) return "Số lượng phòng phải lớn hơn 0.";
        if (room.getAreaSqm() != null && room.getAreaSqm() <= 0) return "Diện tích phải lớn hơn 0.";
        if (room.getStatus() != null && !room.getStatus().equalsIgnoreCase("AVAILABLE") && !room.getStatus().equalsIgnoreCase("MAINTENANCE")) return "Trạng thái phòng không hợp lệ.";
        return null;
    }
}

