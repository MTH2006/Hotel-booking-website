package com.hotelbooking.repository;

import com.hotelbooking.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByHotelIdAndStatusOrderByCreatedAtDesc(Long hotelId, String status);
    List<Review> findByStatusOrderByCreatedAtDesc(String status);
    List<Review> findByUserUuidOrderByCreatedAtDesc(String userUuid);
    Optional<Review> findByBookingId(Long bookingId);
    Optional<Review> findByIdAndUserUuid(Long id, String userUuid);
    List<Review> findByHotelIdAndStatus(Long hotelId, String status);
}
