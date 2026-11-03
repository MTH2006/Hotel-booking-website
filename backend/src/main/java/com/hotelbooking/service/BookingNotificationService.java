
package com.hotelbooking.service;

import com.hotelbooking.model.Booking;
import org.springframework.stereotype.Service;

@Service
public class BookingNotificationService {

    // FR-91: Tao noi dung thong bao xac nhan dat phong
    public String buildBookingConfirmation(Booking booking) {

        if (booking == null || booking.getBookingId() == null) {
            throw new IllegalArgumentException(
                "Thong tin dat phong khong hop le."
            );
        }

        return "Dat phong thanh cong!\n"
                + "Ma dat phong: " + booking.getBookingId() + "\n"
                + "Khach hang: " + booking.getGuestName() + "\n"
                + "Khach san: " + booking.getHotelName() + "\n"
                + "Loai phong: " + booking.getRoomType() + "\n"
                + "Ngay nhan phong: " + booking.getCheckIn() + "\n"
                + "Ngay tra phong: " + booking.getCheckOut() + "\n"
                + "Tong tien: " + booking.getTotalPrice() + " VND";
    }
}
