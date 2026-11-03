
package com.hotelbooking.service;

import com.hotelbooking.model.Booking;
import org.springframework.stereotype.Service;

@Service
public class BookingChangeNotificationService {

    // FR-93: Tao thong bao huy don dat phong
    public String buildCancellationNotification(Booking booking) {

        validateBooking(booking);

        if (!"CANCELLED".equals(booking.getStatus())) {
            throw new IllegalArgumentException(
                "Don dat phong chua duoc huy."
            );
        }

        return "Don dat phong da duoc huy!\n"
                + "Ma dat phong: " + booking.getBookingId() + "\n"
                + "Khach hang: " + booking.getGuestName() + "\n"
                + "Khach san: " + booking.getHotelName() + "\n"
                + "Trang thai: CANCELLED";
    }

    // Kiem tra thong tin don dat phong
    private void validateBooking(Booking booking) {

        if (booking == null
                || booking.getBookingId() == null
                || booking.getBookingId().isBlank()) {

            throw new IllegalArgumentException(
                "Thong tin dat phong khong hop le."
            );
        }
    }
}
