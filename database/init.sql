-- ==============================================================================
-- HOTEL BOOKING SYSTEM - DATABASE INITIALIZATION SCRIPT (PostgreSQL)
-- ==============================================================================

-- Connect to database (if running through psql command line)
-- \connect hotel_booking_db

-- Clean up existing tables if necessary
DROP TABLE IF EXISTS support_requests CASCADE;
DROP TABLE IF EXISTS payments CASCADE;
DROP TABLE IF EXISTS promotions CASCADE;
DROP TABLE IF EXISTS reviews CASCADE;
DROP TABLE IF EXISTS bookings CASCADE;
DROP TABLE IF EXISTS rooms CASCADE;
DROP TABLE IF EXISTS hotels CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- ==============================================================================
-- 1. TABLE DEFINITIONS
-- ==============================================================================

-- Bảng Users (Người dùng / Khách hàng / Nhân viên / Quản trị viên) [FR-1, FR-62, FR-79-84]
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    user_uuid VARCHAR(64) UNIQUE NOT NULL,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    role VARCHAR(20) DEFAULT 'USER' CHECK (role IN ('USER', 'STAFF', 'MANAGER', 'ADMIN')),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Bảng Hotels (Khách sạn) [FR-10-14, FR-56-62]
CREATE TABLE hotels (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    badge VARCHAR(100),
    badge_type VARCHAR(50),
    rating NUMERIC(3, 1) DEFAULT 5.0 CHECK (rating >= 0.0 AND rating <= 5.0),
    reviews_count INTEGER DEFAULT 0 CHECK (reviews_count >= 0),
    location VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    price BIGINT NOT NULL CHECK (price >= 0),
    original_price BIGINT CHECK (original_price IS NULL OR original_price >= price),
    image VARCHAR(500),
    description TEXT,
    address VARCHAR(255),
    phone VARCHAR(50),
    email VARCHAR(100),
    images TEXT,
    amenities TEXT,
    stars INTEGER,
    verified BOOLEAN DEFAULT FALSE,
    checkin VARCHAR(50),
    checkout VARCHAR(50),
    cancellation_policy TEXT,
    stay_policy TEXT,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Bảng Rooms (Hạng phòng thuộc khách sạn) [FR-15-20, FR-63-70]
CREATE TABLE rooms (
    id BIGSERIAL PRIMARY KEY,
    hotel_id BIGINT NOT NULL REFERENCES hotels(id) ON DELETE CASCADE,
    room_type VARCHAR(100) NOT NULL,
    price_per_night BIGINT NOT NULL CHECK (price_per_night > 0),
    capacity INTEGER NOT NULL DEFAULT 2 CHECK (capacity > 0),
    total_rooms INTEGER NOT NULL DEFAULT 10 CHECK (total_rooms > 0),
    area_sqm NUMERIC(6, 2) DEFAULT 30.0,
    bed_type VARCHAR(100) DEFAULT '1 Giường đôi lớn (King)',
    capacity_adults INTEGER DEFAULT 2,
    amenities TEXT DEFAULT 'Wifi miễn phí, Điều hòa, Smart TV, Tủ lạnh mini, Két sắt an toàn',
    image_url VARCHAR(500),
    status VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Bảng Promotions (Chương trình khuyến mãi & Mã giảm giá) [FR-51-55]
CREATE TABLE promotions (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    title TEXT,
    discount_percent NUMERIC(5, 2) NOT NULL CHECK (discount_percent > 0 AND discount_percent <= 100),
    max_discount_amount BIGINT,
    min_booking_amount BIGINT DEFAULT 0,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    usage_limit INTEGER DEFAULT 100,
    used_count INTEGER DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_promotion_dates CHECK (end_date >= start_date)
);

-- Bảng Bookings (Đơn đặt phòng) [FR-21-37, FR-71-78]
CREATE TABLE bookings (
    id BIGSERIAL PRIMARY KEY,
    booking_id VARCHAR(50) UNIQUE NOT NULL,
    hotel_id BIGINT NOT NULL REFERENCES hotels(id) ON DELETE RESTRICT,
    room_id BIGINT REFERENCES rooms(id) ON DELETE SET NULL,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    hidden_from_user BOOLEAN NOT NULL DEFAULT FALSE,
    hotel_name VARCHAR(255) NOT NULL,
    guest_name VARCHAR(150) NOT NULL,
    guest_phone VARCHAR(20) NOT NULL,
    guest_email VARCHAR(255) NOT NULL,
    check_in DATE NOT NULL,
    check_out DATE NOT NULL,
    room_type VARCHAR(100) NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 1 CHECK (quantity > 0),
    total_price BIGINT NOT NULL CHECK (total_price >= 0),
    status VARCHAR(30) DEFAULT 'CONFIRMED' CHECK (status IN ('CONFIRMED', 'CANCELLED', 'CHECKED_IN', 'COMPLETED', 'PENDING')),
    adults INTEGER DEFAULT 2,
    children INTEGER DEFAULT 0,
    special_requests TEXT,
    promotion_code VARCHAR(50),
    discount_amount BIGINT DEFAULT 0,
    payment_method VARCHAR(50) DEFAULT 'CASH_ON_ARRIVAL',
    payment_status VARCHAR(30) DEFAULT 'UNPAID',
    cancellation_reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_booking_dates CHECK (check_out > check_in)
);

-- Bảng Reviews (Đánh giá khách sạn) [FR-45-50]
CREATE TABLE reviews (
    id BIGSERIAL PRIMARY KEY,
    hotel_id BIGINT NOT NULL REFERENCES hotels(id) ON DELETE CASCADE,
    booking_id BIGINT NOT NULL UNIQUE REFERENCES bookings(id) ON DELETE CASCADE,
    user_uuid VARCHAR(64) NOT NULL REFERENCES users(user_uuid) ON DELETE CASCADE,
    author_name VARCHAR(150) NOT NULL,
    rating INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment TEXT NOT NULL CHECK (char_length(trim(comment)) BETWEEN 5 AND 3000),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PUBLISHED', 'REJECTED')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Bảng Payments (Giao dịch thanh toán) [FR-38-44]
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    transaction_id VARCHAR(64) UNIQUE NOT NULL,
    booking_id VARCHAR(50) NOT NULL,
    amount BIGINT NOT NULL CHECK (amount >= 0),
    payment_method VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'SUCCESS',
    payment_gateway VARCHAR(50) DEFAULT 'MOCK_GATEWAY_VN',
    note TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    refunded_at TIMESTAMP WITH TIME ZONE
);

-- Bảng Support Requests (Yêu cầu hỗ trợ khách hàng) [FR-94-96]
CREATE TABLE support_requests (
    id BIGSERIAL PRIMARY KEY,
    customer_name VARCHAR(150) NOT NULL,
    customer_email VARCHAR(255) NOT NULL,
    subject VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ACCEPTED', 'RESOLVED')),
    response_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP WITH TIME ZONE
);

-- ==============================================================================
-- 2. INDEXES FOR PERFORMANCE OPTIMIZATION
-- ==============================================================================
CREATE INDEX idx_hotels_city ON hotels(city);
CREATE INDEX idx_hotels_name ON hotels(name);
CREATE INDEX idx_rooms_hotel_id ON rooms(hotel_id);
CREATE INDEX idx_bookings_hotel_id ON bookings(hotel_id);
CREATE INDEX idx_bookings_dates ON bookings(hotel_id, check_in, check_out);
CREATE INDEX idx_bookings_guest_email ON bookings(guest_email);
CREATE INDEX idx_bookings_user_id ON bookings(user_id);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_reviews_hotel_status ON reviews(hotel_id, status, created_at DESC);
CREATE INDEX idx_reviews_user ON reviews(user_uuid);
CREATE INDEX idx_promotions_code ON promotions(code);
CREATE INDEX idx_payments_booking_id ON payments(booking_id);
CREATE INDEX idx_payments_transaction_id ON payments(transaction_id);
CREATE INDEX idx_support_requests_status ON support_requests(status);

-- ==============================================================================
-- 3. SEED DATA
-- ==============================================================================

-- 3.1 Khách sạn mẫu [FR-10]
INSERT INTO hotels (id, name, badge, badge_type, rating, reviews_count, location, city, price, original_price, image, description, address, phone, email, stars, verified, active) VALUES
(1, 'Grand Vista Hotel', 'Được yêu thích', 'featured', 4.8, 256, 'Quận 1, TP. Hồ Chí Minh', 'TP. Hồ Chí Minh', 1850000, NULL, 'assets/images/hotel_grand_vista.jpg', 'Khách sạn cao cấp giữa lòng Sài Gòn với tầm nhìn toàn cảnh sông Sài Gòn và dịch vụ 5 sao sang trọng.', '123 Đồng Khởi, Bến Nghé, Quận 1', '028 3822 5555', 'reservation@grandvista.vn', 5, true, true),
(2, 'Sunrise Resort Phú Quốc', 'Giảm giá 20%', 'discount', 4.7, 312, 'Phú Quốc, Kiên Giang', 'Phú Quốc', 2500000, 3125000, 'assets/images/hotel_sunrise_resort.jpg', 'Resort nghỉ dưỡng bên bãi biển cát trắng tinh khôi Phú Quốc với hồ bơi vô cực và khuôn viên nhiệt đới xanh mát.', 'Bãi Trường, Dương Tơ, TP. Phú Quốc', '0297 3988 888', 'contact@sunriseresort.vn', 5, true, true),
(3, 'The Lumiere Hotel', 'Phổ biến', 'popular', 4.6, 196, 'Đà Nẵng', 'Đà Nẵng', 1650000, NULL, 'assets/images/hotel_the_lumiere.jpg', 'Toạ lạc ngay bờ sông Hàn và trung tâm thành phố Đà Nẵng, thiết kế hiện đại với Sky Bar và hồ bơi tầng thượng.', '88 Bạch Đằng, Hải Châu, Đà Nẵng', '0236 3844 666', 'booking@lumierehotel.vn', 4, true, true),
(4, 'Melia Vinpearl Nha Trang', 'Mới', 'new', 4.9, 420, 'Nha Trang, Khánh Hòa', 'Nha Trang', 2800000, NULL, 'assets/images/hotel_melia_nha_trang.jpg', 'Tổ hợp nghỉ dưỡng chuẩn quốc tế hướng biển Nha Trang với bãi biển riêng và hệ thống ẩm thực thượng hạng.', 'Đảo Hòn Tre, Vĩnh Nguyên, TP. Nha Trang', '0258 3598 999', 'welcome@meliavinpearl.vn', 5, true, true);

SELECT setval('hotels_id_seq', (SELECT MAX(id) FROM hotels));

-- 3.2 Hạng phòng mẫu [FR-17, FR-63]
INSERT INTO rooms (id, hotel_id, room_type, price_per_night, capacity, total_rooms, area_sqm, bed_type, capacity_adults, amenities, image_url, status, is_active, description) VALUES
(1, 1, 'Deluxe King City View', 1850000, 2, 15, 38.0, '1 Giường đôi lớn (King)', 2, 'Wifi miễn phí, Điều hòa, Smart TV, Tủ lạnh mini, Bồn tắm', 'assets/images/hotel_grand_vista.jpg', 'AVAILABLE', true, 'Phòng Deluxe giường King với cửa sổ kính view toàn cảnh thành phố Sài Gòn hoa lệ.'),
(2, 1, 'Executive Suite', 3200000, 3, 5, 65.0, '1 Giường King + 1 Sofa bed', 3, 'Wifi miễn phí, Quyền vào Executive Lounge, Bồn tắm Jacuzzi, Máy pha cà phê cao cấp', 'assets/images/hotel_grand_vista.jpg', 'AVAILABLE', true, 'Phòng Suite cao cấp với phòng khách riêng biệt và quyền truy cập đặc quyền Executive Lounge.'),
(3, 2, 'Ocean Front Bungalow', 2500000, 2, 20, 50.0, '1 Giường King hướng biển', 2, 'Hiên tắm nắng riêng, Hồ bơi vô cực, Wifi, Bữa sáng hải sản', 'assets/images/hotel_sunrise_resort.jpg', 'AVAILABLE', true, 'Bungalow sát bờ biển với hiên tắm nắng ngắm trọn hoàng hôn biển đảo ngọc.'),
(4, 2, 'Family Beach Villa', 4800000, 4, 8, 110.0, '2 Giường King (2 phòng ngủ)', 4, 'Hồ bơi riêng mini, Bếp nhỏ, Phòng khách rộng, Xe đưa đón sân bay', 'assets/images/hotel_sunrise_resort.jpg', 'AVAILABLE', true, 'Biệt thự biển sang trọng 2 phòng ngủ cho gia đình kèm hồ bơi nhỏ biệt lập.'),
(5, 3, 'Premier River View', 1650000, 2, 25, 35.0, '1 Giường Queen hoặc 2 Giường đơn', 2, 'Wifi tốc độ cao, Ban công ngắm cầu Rồng, Điều hòa 2 chiều', 'assets/images/hotel_the_lumiere.jpg', 'AVAILABLE', true, 'Phòng tiêu chuẩn ban công rộng nhìn thẳng ra sông Hàn và cầu Rồng lung linh về đêm.'),
(6, 4, 'Grand Ocean Suite', 2800000, 2, 12, 55.0, '1 Giường King siêu lớn', 2, 'Bồn tắm nằm view vịnh Nha Trang, Wifi, Rượu vang đón khách', 'assets/images/hotel_melia_nha_trang.jpg', 'AVAILABLE', true, 'Suite cao cấp hướng biển Nha Trang với bồn tắm kính ngắm bình minh biển.');

SELECT setval('rooms_id_seq', (SELECT MAX(id) FROM rooms));

-- 3.3 Người dùng mẫu (Mật khẩu 'Password@123' dạng sha256)
INSERT INTO users (id, user_uuid, name, email, password_hash, phone, role, is_active) VALUES
(1, 'usr_admin_001', 'Hệ Thống Quản Trị', 'admin@hotelbooking.com', 'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f', '0909999999', 'ADMIN', true),
(2, 'usr_guest_002', 'Nguyễn Văn An', 'nguyenvanan@gmail.com', 'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f', '0901234567', 'USER', true),
(3, 'usr_staff_003', 'Trần Thị Lễ Tân', 'staff@hotelbooking.com', 'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f', '0908888888', 'STAFF', true);

SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));

-- 3.4 Khuyến mãi mẫu [FR-51]
INSERT INTO promotions (id, code, title, discount_percent, max_discount_amount, min_booking_amount, start_date, end_date, usage_limit, used_count, is_active) VALUES
(1, 'HESTORA2026', 'Chào mừng năm 2026 - Giảm ngay 10%', 10.0, 500000, 1000000, '2026-01-01', '2026-12-31', 500, 15, true),
(2, 'SUMMERHOT', 'Ưu đãi hè rực rỡ - Giảm ngay 15%', 15.0, 800000, 2000000, '2026-05-01', '2026-09-30', 200, 42, true),
(3, 'VIPSTAY', 'Đặc quyền thành viên VIP - Giảm 20%', 20.0, 1500000, 3000000, '2026-01-01', '2026-12-31', 100, 8, true);

SELECT setval('promotions_id_seq', (SELECT MAX(id) FROM promotions));

-- 3.5 Đơn đặt phòng mẫu [FR-29]
INSERT INTO bookings (id, booking_id, hotel_id, room_id, user_id, hotel_name, guest_name, guest_phone, guest_email, check_in, check_out, room_type, total_price, status, payment_method, payment_status) VALUES
(1, 'HB-821934', 1, 1, 2, 'Grand Vista Hotel', 'Nguyễn Văn An', '0901234567', 'nguyenvanan@gmail.com', '2026-11-10', '2026-11-12', 'Deluxe King City View', 3700000, 'CONFIRMED', 'CASH_ON_ARRIVAL', 'UNPAID'),
(2, 'HB-731209', 2, 3, 2, 'Sunrise Resort Phú Quốc', 'Nguyễn Văn An', '0901234567', 'nguyenvanan@gmail.com', '2026-10-01', '2026-10-04', 'Ocean Front Bungalow', 7500000, 'COMPLETED', 'VNPAY', 'PAID'),
(3, 'HB-612345', 3, 5, 2, 'The Lumiere Hotel', 'Nguyễn Văn An', '0901234567', 'nguyenvanan@gmail.com', '2026-12-05', '2026-12-07', 'Premier River View', 3300000, 'CONFIRMED', 'CREDIT_CARD', 'PAID');

SELECT setval('bookings_id_seq', (SELECT MAX(id) FROM bookings));

-- 3.6 Giao dịch thanh toán mẫu [FR-42]
INSERT INTO payments (transaction_id, booking_id, amount, payment_method, status, payment_gateway, note) VALUES
('TXN-1730000000-1001', 'HB-731209', 7500000, 'VNPAY', 'SUCCESS', 'VNPAY_GATEWAY', 'Thanh toán trực tuyến thành công qua cổng VNPAY QR'),
('TXN-1730000000-1002', 'HB-612345', 3300000, 'CREDIT_CARD', 'SUCCESS', 'ONEPAY_GATEWAY', 'Thanh toán thẻ Visa quốc tế');

-- 3.7 Đánh giá mẫu [FR-45]
INSERT INTO reviews (hotel_id, booking_id, user_uuid, author_name, rating, comment, status) VALUES
(2, 2, 'usr_guest_002', 'Nguyễn Văn An', 5, 'Kỳ nghỉ tuyệt vời ngoài sức tưởng tượng! Resort cực kỳ đẹp, phòng sát biển nhìn hoàng hôn lãng mạn. Nhân viên thân thiện và đồ ăn rất ngon.', 'PUBLISHED');

-- 3.8 Yêu cầu hỗ trợ mẫu [FR-94]
INSERT INTO support_requests (customer_name, customer_email, subject, message, status) VALUES
('Nguyễn Văn An', 'nguyenvanan@gmail.com', 'Yêu cầu phòng tầng cao view biển', 'Tôi đã đặt đơn HB-821934, mong khách sạn hỗ trợ sắp xếp phòng ở tầng 15 trở lên. Cảm ơn!', 'PENDING');
