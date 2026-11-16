package com.hotelbooking.service;

import com.hotelbooking.model.Booking;
import com.hotelbooking.model.Hotel;
import com.hotelbooking.model.Review;
import com.hotelbooking.repository.BookingRepository;
import com.hotelbooking.repository.HotelRepository;
import com.hotelbooking.repository.ReviewRepository;
import com.hotelbooking.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReviewService {
    private final ReviewRepository reviews;
    private final BookingRepository bookings;
    private final HotelRepository hotels;
    private final UserRepository users;

    public ReviewService(ReviewRepository reviews, BookingRepository bookings, HotelRepository hotels, UserRepository users) {
        this.reviews = reviews; this.bookings = bookings; this.hotels = hotels; this.users = users;
    }

    public List<Review> published(Long hotelId) { return reviews.findByHotelIdAndStatusOrderByCreatedAtDesc(hotelId, "PUBLISHED"); }
    public List<Review> mine(String uuid) { return reviews.findByUserUuidOrderByCreatedAtDesc(uuid); }
    public List<Review> pending() { return reviews.findByStatusOrderByCreatedAtDesc("PENDING"); }

    @Transactional
    public Review create(String uuid, Long bookingId, Integer rating, String comment) {
        Booking booking = bookings.findById(bookingId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn đặt phòng."));
        var user = users.findByUserUuid(uuid).orElseThrow(() -> new IllegalArgumentException("Tài khoản không hợp lệ."));
        if (!booking.getGuestEmail().equalsIgnoreCase(user.getEmail())) throw new IllegalArgumentException("Đơn đặt phòng không thuộc tài khoản này.");
        if (!"COMPLETED".equals(booking.getStatus()) || booking.getCheckOut().isAfter(LocalDate.now())) throw new IllegalArgumentException("Chỉ có thể đánh giá sau khi lưu trú đã hoàn thành.");
        if (reviews.findByBookingId(bookingId).isPresent()) throw new IllegalArgumentException("Đơn đặt phòng này đã được đánh giá.");
        validate(rating, comment);
        Review review = new Review(); review.setHotelId(booking.getHotelId()); review.setBookingId(bookingId);
        review.setUserUuid(uuid); review.setAuthorName(user.getName()); review.setRating(rating); review.setComment(comment.trim());
        return reviews.save(review);
    }

    @Transactional
    public Review edit(String uuid, Long id, Integer rating, String comment) {
        Review review = reviews.findByIdAndUserUuid(id, uuid).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đánh giá của bạn."));
        if ("REJECTED".equals(review.getStatus())) throw new IllegalArgumentException("Đánh giá đã bị từ chối nên không thể chỉnh sửa.");
        validate(rating, comment); review.setRating(rating); review.setComment(comment.trim());
        review.setStatus("PENDING"); review.setUpdatedAt(LocalDateTime.now()); Review saved = reviews.save(review); refreshRating(review.getHotelId()); return saved;
    }

    @Transactional
    public void delete(String uuid, Long id) {
        Review review = reviews.findByIdAndUserUuid(id, uuid).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đánh giá của bạn."));
        Long hotelId = review.getHotelId(); reviews.delete(review); refreshRating(hotelId);
    }

    @Transactional
    public Review moderate(Long id, String status) {
        if (status == null || !List.of("PUBLISHED", "REJECTED").contains(status)) throw new IllegalArgumentException("Trạng thái kiểm duyệt không hợp lệ.");
        Review review = reviews.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đánh giá."));
        review.setStatus(status); review.setUpdatedAt(LocalDateTime.now()); Review saved = reviews.save(review); refreshRating(review.getHotelId()); return saved;
    }

    private void validate(Integer rating, String comment) {
        if (rating == null || rating < 1 || rating > 5) throw new IllegalArgumentException("Điểm đánh giá phải từ 1 đến 5.");
        if (comment == null || comment.trim().length() < 5 || comment.trim().length() > 3000) throw new IllegalArgumentException("Nhận xét cần từ 5 đến 3000 ký tự.");
    }

    private void refreshRating(Long hotelId) {
        List<Review> published = reviews.findByHotelIdAndStatus(hotelId, "PUBLISHED");
        hotels.findById(hotelId).ifPresent(hotel -> {
            hotel.setReviewsCount(published.size());
            hotel.setRating(published.isEmpty() ? 0.0 : Math.round(published.stream().mapToInt(Review::getRating).average().orElse(0) * 10.0) / 10.0);
            hotels.save(hotel);
        });
    }
}
