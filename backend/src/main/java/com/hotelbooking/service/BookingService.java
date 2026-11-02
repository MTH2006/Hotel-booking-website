package com.hotelbooking.service;

import com.hotelbooking.model.Booking;
import com.hotelbooking.model.Room;
import com.hotelbooking.repository.BookingRepository;
import com.hotelbooking.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private RoomRepository roomRepository;

    public List<Booking> getBookingsByUserId(Long userId) {
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Booking getBookingById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt phòng với ID: " + id));
    }

    @Transactional
    public Booking createBooking(Booking bookingRequest) {
        // 1. Kiểm tra ngày đặt
        if (bookingRequest.getCheckOutDate().isBefore(bookingRequest.getCheckInDate()) || 
            bookingRequest.getCheckInDate().equals(bookingRequest.getCheckOutDate())) {
            throw new IllegalArgumentException("Ngày trả phòng phải sau ngày nhận phòng.");
        }

        // 2. Lấy thông tin phòng
        Room room = roomRepository.findById(bookingRequest.getRoom().getId())
                .orElseThrow(() -> new RuntimeException("Phòng không tồn tại"));

        // 3. Kiểm tra xem phòng có bị trùng lịch hay không
        boolean isBooked = bookingRepository.isRoomBookedInPeriod(
                room.getId(), bookingRequest.getCheckInDate(), bookingRequest.getCheckOutDate());
        
        if (isBooked) {
            throw new IllegalStateException("Phòng đã có người đặt trong khoảng thời gian này!");
        }

        // 4. Tính tổng tiền: số đêm * giá phòng/đêm
        long nights = ChronoUnit.DAYS.between(bookingRequest.getCheckInDate(), bookingRequest.getCheckOutDate());
        BigDecimal totalPrice = room.getPricePerNight().multiply(BigDecimal.valueOf(nights));

        bookingRequest.setRoom(room);
        bookingRequest.setTotalPrice(totalPrice);
        bookingRequest.setStatus(Booking.BookingStatus.CONFIRMED);

        return bookingRepository.save(bookingRequest);
    }

    @Transactional
    public Booking cancelBooking(Long bookingId) {
        Booking booking = getBookingById(bookingId);
        if (booking.getStatus() == Booking.BookingStatus.CANCELLED) {
            throw new IllegalStateException("Đơn đặt phòng này đã bị hủy trước đó.");
        }
        booking.setStatus(Booking.BookingStatus.CANCELLED);
        return bookingRepository.save(booking);
    }
}