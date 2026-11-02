package com.hotelbooking.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Entity
@Table(name = "rooms")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hotel_id", nullable = false)
    private Long hotelId;

    @Column(name = "room_type_name", nullable = false)
    private String roomTypeName; // Tên loại phòng [FR-63, FR-64]

    private Double areaSqm; // Diện tích (m2) [FR-64]
    private String bedType; // Loại giường [FR-64]
    private Integer capacityAdults; // Sức chứa [FR-64]
    
    @Column(nullable = false)
    private BigDecimal pricePerNight; // Giá phòng [FR-65]

    @Column(nullable = false)
    private Integer totalQuantity; // Số lượng phòng [FR-66]

    private String amenities; // Tiện nghi [FR-67]
    private String imageUrl; // Hình ảnh [FR-68]

    @Column(nullable = false)
    private String status = "AVAILABLE"; // AVAILABLE, MAINTENANCE [FR-69]

    @Column(nullable = false)
    private Boolean isActive = true; // TRUE: kinh doanh, FALSE: ngừng kinh doanh [FR-70]
}