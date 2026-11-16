package com.hotelbooking.repository;

import com.hotelbooking.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

        Optional<Booking> findByBookingId(String bookingId);

        List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);

        List<Booking> findByUserIdAndHiddenFromUserFalseOrderByCreatedAtDesc(Long userId);

        List<Booking> findByGuestEmailOrderByCreatedAtDesc(String guestEmail);

        List<Booking> findAllByOrderByCreatedAtDesc();

        List<Booking> findByHotelIdOrderByCreatedAtDesc(Long hotelId);

        List<Booking> findByStatusOrderByCreatedAtDesc(String status);

        List<Booking> findByGuestEmailIgnoreCaseAndStatusAndCheckOutLessThanEqualOrderByCheckOutDesc(
                        String guestEmail, String status, LocalDate checkOut);

        @Query("SELECT COUNT(b) FROM Booking b WHERE " +
                        "b.hotelId = :hotelId AND " +
                        "b.roomType = :roomType AND " +
                        "b.status NOT IN ('CANCELLED') AND " +
                        "b.checkIn < :checkOut AND " +
                        "b.checkOut > :checkIn")
        long countOverlappingBookings(
                        @Param("hotelId") Long hotelId,
                        @Param("roomType") String roomType,
                        @Param("checkIn") LocalDate checkIn,
                        @Param("checkOut") LocalDate checkOut);

        @Query("SELECT COUNT(b) > 0 FROM Booking b WHERE " +
                        "b.roomId = :roomId AND " +
                        "b.status NOT IN ('CANCELLED') AND " +
                        "b.checkIn < :checkOut AND " +
                        "b.checkOut > :checkIn")
        boolean isRoomBookedInPeriod(
                        @Param("roomId") Long roomId,
                        @Param("checkIn") LocalDate checkIn,
                        @Param("checkOut") LocalDate checkOut);

        @Query("SELECT b FROM Booking b WHERE " +
                        "(:keyword IS NULL OR LOWER(b.bookingId) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                        "OR LOWER(b.guestName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                        "OR LOWER(b.guestEmail) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                        "OR LOWER(b.guestPhone) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                        "OR LOWER(b.hotelName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
                        "AND (:hotelId IS NULL OR b.hotelId = :hotelId) " +
                        "AND (:status IS NULL OR b.status = :status) " +
                        "ORDER BY b.createdAt DESC")
        List<Booking> searchStaffBookings(
                        @Param("keyword") String keyword,
                        @Param("hotelId") Long hotelId,
                        @Param("status") String status);
}
