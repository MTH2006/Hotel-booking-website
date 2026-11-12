package com.hotelbooking.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
public class Booking {

    public static class BookingStatus {
        public static final String PENDING = "PENDING";
        public static final String CONFIRMED = "CONFIRMED";
        public static final String CHECKED_IN = "CHECKED_IN";
        public static final String COMPLETED = "COMPLETED";
        public static final String CANCELLED = "CANCELLED";
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id", unique = true, nullable = false, length = 50)
    private String bookingId;

    @Column(name = "hotel_id", nullable = false)
    private Long hotelId;

    @Column(name = "room_id")
    private Long roomId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "hidden_from_user", nullable = false)
    private Boolean hiddenFromUser = false;

    @Column(name = "hotel_name", nullable = false)
    private String hotelName;

    @Column(name = "guest_name", nullable = false, length = 150)
    private String guestName;

    @Column(name = "guest_phone", nullable = false, length = 20)
    private String guestPhone;

    @Column(name = "guest_email", nullable = false)
    private String guestEmail;

    @Column(name = "check_in", nullable = false)
    private LocalDate checkIn;

    @Column(name = "check_out", nullable = false)
    private LocalDate checkOut;

    @Column(name = "room_type", nullable = false, length = 100)
    private String roomType;

    @Column(name = "total_price", nullable = false)
    private Long totalPrice;

    @Column(nullable = false, length = 30)
    private String status = BookingStatus.CONFIRMED;

    @Column(name = "adults")
    private Integer adults = 2;

    @Column(name = "children")
    private Integer children = 0;

    @Column(name = "special_requests", columnDefinition = "TEXT")
    private String specialRequests;

    @Column(name = "promotion_code", length = 50)
    private String promotionCode;

    @Column(name = "discount_amount")
    private Long discountAmount = 0L;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod = "CASH_ON_ARRIVAL"; // CASH_ON_ARRIVAL, VNPAY, CREDIT_CARD

    @Column(name = "payment_status", length = 30)
    private String paymentStatus = "UNPAID"; // UNPAID, PAID, REFUNDED

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Transient
    private Room room;

    public Booking() {
        this.createdAt = LocalDateTime.now();
        this.status = BookingStatus.CONFIRMED;
    }

    public Booking(String bookingId, Long hotelId, String hotelName, String guestName, String guestPhone, String guestEmail, LocalDate checkIn, LocalDate checkOut, String roomType, Long totalPrice) {
        this.bookingId = bookingId;
        this.hotelId = hotelId;
        this.hotelName = hotelName;
        this.guestName = guestName;
        this.guestPhone = guestPhone;
        this.guestEmail = guestEmail;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.roomType = roomType;
        this.totalPrice = totalPrice;
        this.status = BookingStatus.CONFIRMED;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public Long getHotelId() { return hotelId; }
    public void setHotelId(Long hotelId) { this.hotelId = hotelId; }

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Boolean getHiddenFromUser() { return hiddenFromUser; }
    public void setHiddenFromUser(Boolean hiddenFromUser) { this.hiddenFromUser = hiddenFromUser; }

    public String getHotelName() { return hotelName; }
    public void setHotelName(String hotelName) { this.hotelName = hotelName; }

    public String getGuestName() { return guestName; }
    public void setGuestName(String guestName) { this.guestName = guestName; }

    public String getGuestPhone() { return guestPhone; }
    public void setGuestPhone(String guestPhone) { this.guestPhone = guestPhone; }

    public String getGuestEmail() { return guestEmail; }
    public void setGuestEmail(String guestEmail) { this.guestEmail = guestEmail; }

    public LocalDate getCheckIn() { return checkIn; }
    public void setCheckIn(LocalDate checkIn) { this.checkIn = checkIn; }

    public LocalDate getCheckOut() { return checkOut; }
    public void setCheckOut(LocalDate checkOut) { this.checkOut = checkOut; }

    // Date aliases
    public LocalDate getCheckInDate() { return checkIn; }
    public void setCheckInDate(LocalDate checkInDate) { this.checkIn = checkInDate; }

    public LocalDate getCheckOutDate() { return checkOut; }
    public void setCheckOutDate(LocalDate checkOutDate) { this.checkOut = checkOutDate; }

    public String getRoomType() { return roomType; }
    public void setRoomType(String roomType) { this.roomType = roomType; }

    public Long getTotalPrice() { return totalPrice; }
    public void setTotalPrice(Long totalPrice) { this.totalPrice = totalPrice; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getAdults() { return adults; }
    public void setAdults(Integer adults) { this.adults = adults; }

    public Integer getChildren() { return children; }
    public void setChildren(Integer children) { this.children = children; }

    public String getSpecialRequests() { return specialRequests; }
    public void setSpecialRequests(String specialRequests) { this.specialRequests = specialRequests; }

    public String getPromotionCode() { return promotionCode; }
    public void setPromotionCode(String promotionCode) { this.promotionCode = promotionCode; }

    public Long getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(Long discountAmount) { this.discountAmount = discountAmount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Room getRoom() { return room; }
    public void setRoom(Room room) {
        this.room = room;
        if (room != null) {
            this.roomId = room.getId();
            this.roomType = room.getRoomType();
            if (this.hotelId == null) {
                this.hotelId = room.getHotelId();
            }
        }
    }
}
