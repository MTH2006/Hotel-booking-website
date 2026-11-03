package com.hotelbooking.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "rooms")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hotel_id", nullable = false)
    private Long hotelId;

    @Column(name = "room_type", nullable = false, length = 100)
    private String roomType; // Tên loại phòng [FR-63, FR-64]

    @Column(name = "price_per_night", nullable = false)
    private Long pricePerNight; // Giá phòng [FR-65]

    @Column(nullable = false)
    private Integer capacity = 2; // Sức chứa [FR-64]

    @Column(name = "total_rooms", nullable = false)
    private Integer totalRooms = 10; // Số lượng phòng [FR-66]

    @Column(name = "area_sqm")
    private Double areaSqm = 30.0; // Diện tích (m2) [FR-64]

    @Column(name = "bed_type")
    private String bedType = "1 Giường đôi lớn (King)"; // Loại giường [FR-64]

    @Column(name = "capacity_adults")
    private Integer capacityAdults = 2; // Sức chứa người lớn [FR-64]

    @Column(name = "amenities", columnDefinition = "TEXT")
    private String amenities = "Wifi miễn phí, Điều hòa, Smart TV, Tủ lạnh mini, Két sắt an toàn"; // Tiện nghi [FR-67]

    @Column(name = "image_url", length = 500)
    private String imageUrl; // Hình ảnh [FR-68]

    @Column(name = "status", nullable = false, length = 30)
    private String status = "AVAILABLE"; // AVAILABLE, MAINTENANCE [FR-69]

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true; // TRUE: kinh doanh, FALSE: ngừng [FR-70]

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public Room() {
        this.createdAt = LocalDateTime.now();
        this.status = "AVAILABLE";
        this.isActive = true;
    }

    public Room(Long id, Long hotelId, String roomType, Long pricePerNight, Integer capacity, Integer totalRooms, String description) {
        this.id = id;
        this.hotelId = hotelId;
        this.roomType = roomType;
        this.pricePerNight = pricePerNight;
        this.capacity = capacity;
        this.totalRooms = totalRooms;
        this.description = description;
        this.capacityAdults = capacity;
        this.createdAt = LocalDateTime.now();
        this.status = "AVAILABLE";
        this.isActive = true;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getHotelId() { return hotelId; }
    public void setHotelId(Long hotelId) { this.hotelId = hotelId; }

    public String getRoomType() { return roomType; }
    public void setRoomType(String roomType) { this.roomType = roomType; }

    // Alias for FR-63/64
    public String getRoomTypeName() { return roomType; }
    public void setRoomTypeName(String roomTypeName) { this.roomType = roomTypeName; }

    public Long getPricePerNight() { return pricePerNight; }
    public void setPricePerNight(Long pricePerNight) { this.pricePerNight = pricePerNight; }
    public void setPricePerNight(BigDecimal pricePerNight) {
        this.pricePerNight = pricePerNight != null ? pricePerNight.longValue() : 0L;
    }

    public BigDecimal getPricePerNightBigDecimal() {
        return pricePerNight != null ? BigDecimal.valueOf(pricePerNight) : BigDecimal.ZERO;
    }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
        this.capacityAdults = capacity;
    }

    public Integer getTotalRooms() { return totalRooms; }
    public void setTotalRooms(Integer totalRooms) { this.totalRooms = totalRooms; }

    // Alias for totalQuantity
    public Integer getTotalQuantity() { return totalRooms; }
    public void setTotalQuantity(Integer totalQuantity) { this.totalRooms = totalQuantity; }

    public Double getAreaSqm() { return areaSqm; }
    public void setAreaSqm(Double areaSqm) { this.areaSqm = areaSqm; }

    public String getBedType() { return bedType; }
    public void setBedType(String bedType) { this.bedType = bedType; }

    public Integer getCapacityAdults() { return capacityAdults; }
    public void setCapacityAdults(Integer capacityAdults) { this.capacityAdults = capacityAdults; }

    public String getAmenities() { return amenities; }
    public void setAmenities(String amenities) { this.amenities = amenities; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
