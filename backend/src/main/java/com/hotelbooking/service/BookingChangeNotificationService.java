
package com.hotelbooking.service;

import com.hotelbooking.model.Booking;
import org.springframework.stereotype.Service;

import java.util.Objects;

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

    // FR-93: Tao thong bao khi thong tin dat phong thay doi
    public String buildBookingUpdateNotification(
            Booking before,
            Booking after) {

        validateBooking(before);
        validateBooking(after);

        // Dam bao hai doi tuong thuoc cung mot don dat phong
        if (!before.getBookingId().equals(after.getBookingId())) {
            throw new IllegalArgumentException(
                    "Hai don dat phong khong trung khop."
            );
        }

        StringBuilder message = new StringBuilder();

        message.append("Thong tin dat phong da thay doi!\n");
        message.append("Ma dat phong: ")
                .append(after.getBookingId())
                .append("\n");

        boolean hasChanges = false;

        // 1. Kiem tra thay doi ngay nhan phong
        if (!Objects.equals(
                before.getCheckIn(),
                after.getCheckIn())) {

            message.append("Ngay nhan phong: ")
                    .append(before.getCheckIn())
                    .append(" -> ")
                    .append(after.getCheckIn())
                    .append("\n");

            hasChanges = true;
        }

        // 2. Kiem tra thay doi ngay tra phong
        if (!Objects.equals(
                before.getCheckOut(),
                after.getCheckOut())) {

            message.append("Ngay tra phong: ")
                    .append(before.getCheckOut())
                    .append(" -> ")
                    .append(after.getCheckOut())
                    .append("\n");

            hasChanges = true;
        }

        // 3. Kiem tra thay doi loai phong
        if (!Objects.equals(
                before.getRoomType(),
                after.getRoomType())) {

            message.append("Loai phong: ")
                    .append(before.getRoomType())
                    .append(" -> ")
                    .append(after.getRoomType())
                    .append("\n");

            hasChanges = true;
        }

        // 4. Kiem tra thay doi trang thai dat phong
        if (!Objects.equals(
                before.getStatus(),
                after.getStatus())) {

            message.append("Trang thai: ")
                    .append(before.getStatus())
                    .append(" -> ")
                    .append(after.getStatus())
                    .append("\n");

            hasChanges = true;
        }

        // 5. Kiem tra thay doi tong tien
        if (!Objects.equals(
                before.getTotalPrice(),
                after.getTotalPrice())) {

            message.append("Tong tien: ")
                    .append(before.getTotalPrice())
                    .append(" -> ")
                    .append(after.getTotalPrice())
                    .append(" VND\n");

            hasChanges = true;
        }

        // Khong tao thong bao khi du lieu khong thay doi
        if (!hasChanges) {
            throw new IllegalArgumentException(
                    "Thong tin dat phong khong co thay doi."
            );
        }

        return message.toString();
    }

    // FR-93: Kiem tra thong tin don dat phong
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
