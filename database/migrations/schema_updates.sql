-- ==================================================
-- HOTEL BOOKING WEBSITE
-- DATABASE SCHEMA UPDATES & AI RAG ENGINE MIGRATIONS
-- ==================================================

-- 1. Account management: Lock / Unlock users
ALTER TABLE users
ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;

-- 2. Keep existing room rows queryable after newer room availability fields were added.
ALTER TABLE rooms ADD COLUMN IF NOT EXISTS status VARCHAR(30);
ALTER TABLE rooms ALTER COLUMN status SET DEFAULT 'AVAILABLE';
UPDATE rooms SET status = 'AVAILABLE' WHERE status IS NULL;
ALTER TABLE rooms ALTER COLUMN status SET NOT NULL;

ALTER TABLE rooms ADD COLUMN IF NOT EXISTS is_active BOOLEAN;
ALTER TABLE rooms ALTER COLUMN is_active SET DEFAULT TRUE;
UPDATE rooms SET is_active = TRUE WHERE is_active IS NULL;
ALTER TABLE rooms ALTER COLUMN is_active SET NOT NULL;

-- 3. Let customers hide cancelled bookings from their own history without erasing admin records.
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS hidden_from_user BOOLEAN;
ALTER TABLE bookings ALTER COLUMN hidden_from_user SET DEFAULT FALSE;
UPDATE bookings SET hidden_from_user = FALSE WHERE hidden_from_user IS NULL;
ALTER TABLE bookings ALTER COLUMN hidden_from_user SET NOT NULL;

-- ==================================================
-- AI HOTEL ASSISTANT (RAG ENGINE) DATABASE SCHEMA
-- ==================================================

-- 4. Cosine similarity function for Vector Similarity Search in PostgreSQL
CREATE OR REPLACE FUNCTION cosine_similarity(a real[], b real[]) RETURNS real AS $$
DECLARE
    dot_product real := 0;
    norm_a real := 0;
    norm_b real := 0;
    i int;
    dim int;
BEGIN
    IF a IS NULL OR b IS NULL THEN
        RETURN 0;
    END IF;
    dim := array_length(a, 1);
    IF dim IS NULL OR dim != array_length(b, 1) THEN
        RETURN 0;
    END IF;
    FOR i IN 1..dim LOOP
        dot_product := dot_product + a[i] * b[i];
        norm_a := norm_a + a[i] * a[i];
        norm_b := norm_b + b[i] * b[i];
    END LOOP;
    IF norm_a = 0 OR norm_b = 0 THEN
        RETURN 0;
    END IF;
    RETURN dot_product / (sqrt(norm_a) * sqrt(norm_b));
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- 5. AI Conversations
CREATE TABLE IF NOT EXISTS ai_conversations (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(64) UNIQUE NOT NULL,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    title VARCHAR(255) DEFAULT 'Cuộc trò chuyện mới',
    status VARCHAR(30) DEFAULT 'ACTIVE',
    metadata_json TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_ai_conv_session ON ai_conversations(session_id);
CREATE INDEX IF NOT EXISTS idx_ai_conv_user ON ai_conversations(user_id);

-- 6. AI Messages
CREATE TABLE IF NOT EXISTS ai_messages (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL REFERENCES ai_conversations(id) ON DELETE CASCADE,
    sender_type VARCHAR(20) NOT NULL, -- 'USER', 'ASSISTANT', 'SYSTEM'
    content TEXT NOT NULL,
    intent VARCHAR(50) DEFAULT 'GENERAL_FAQ',
    citations TEXT,
    metadata_json TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_ai_msg_conv ON ai_messages(conversation_id);
CREATE INDEX IF NOT EXISTS idx_ai_msg_created ON ai_messages(created_at);

-- 7. AI Knowledge Documents
CREATE TABLE IF NOT EXISTS ai_knowledge_documents (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    category VARCHAR(50) NOT NULL, -- 'HOTEL_INFO', 'ROOM_INFO', 'POLICY', 'PAYMENT', 'FAQ'
    source_type VARCHAR(50) DEFAULT 'MANUAL',
    hotel_id BIGINT REFERENCES hotels(id) ON DELETE SET NULL,
    status VARCHAR(30) DEFAULT 'INDEXED',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_ai_doc_category ON ai_knowledge_documents(category);
CREATE INDEX IF NOT EXISTS idx_ai_doc_hotel ON ai_knowledge_documents(hotel_id);

-- 8. AI Knowledge Chunks
CREATE TABLE IF NOT EXISTS ai_knowledge_chunks (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL REFERENCES ai_knowledge_documents(id) ON DELETE CASCADE,
    chunk_index INTEGER DEFAULT 0,
    content TEXT NOT NULL,
    metadata_json TEXT,
    embedding REAL[],
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_ai_chunk_doc ON ai_knowledge_chunks(document_id);

-- 9. AI Handoff Requests (chuyển tiếp câu hỏi cho nhân viên)
CREATE TABLE IF NOT EXISTS ai_handoff_requests (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT REFERENCES ai_conversations(id) ON DELETE SET NULL,
    customer_name VARCHAR(150),
    customer_email VARCHAR(255),
    customer_phone VARCHAR(20),
    question_summary TEXT NOT NULL,
    status VARCHAR(30) DEFAULT 'PENDING',
    response_note TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP WITH TIME ZONE
);
CREATE INDEX IF NOT EXISTS idx_ai_handoff_status ON ai_handoff_requests(status);

-- ==================================================
-- 10. SEED KNOWLEDGE BASE DOCUMENTS & CHUNKS
-- ==================================================

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM ai_knowledge_documents WHERE id = 1) THEN
        -- Document 1: Giới thiệu HESTORA
        INSERT INTO ai_knowledge_documents (id, title, category, source_type, status)
        VALUES (1, 'Tổng quan thương hiệu HESTORA Hotel Group', 'HOTEL_INFO', 'SYSTEM', 'INDEXED');

        INSERT INTO ai_knowledge_chunks (document_id, chunk_index, content, metadata_json)
        VALUES (1, 0, 'HESTORA là thương hiệu khách sạn và khu nghỉ dưỡng cao cấp đạt chuẩn 4 đến 5 sao tại Việt Nam với slogan "Your Stay, Your Story". HESTORA hiện sở hữu 4 cơ sở khách sạn và resort sang trọng tại TP. Hồ Chí Minh, Phú Quốc, Đà Nẵng và Nha Trang. Hotline hỗ trợ 24/7: 1900 8888, email: contact@hestora.vn.', '{"category":"HOTEL_INFO","brand":"HESTORA"}');

        -- Document 2: Grand Vista Hotel
        INSERT INTO ai_knowledge_documents (id, title, category, source_type, hotel_id, status)
        VALUES (2, 'Khách sạn Grand Vista Hotel - TP. Hồ Chí Minh', 'HOTEL_INFO', 'SYSTEM', 1, 'INDEXED');

        INSERT INTO ai_knowledge_chunks (document_id, chunk_index, content, metadata_json)
        VALUES (2, 0, 'Grand Vista Hotel toạ lạc tại số 123 Đồng Khởi, Bến Nghé, Quận 1, TP. Hồ Chí Minh. Tiêu chuẩn 5 sao quốc tế, 256 phòng, view nhìn toàn cảnh sông Sài Gòn và thành phố. Khách sạn có hồ bơi vô cực trên tầng 28, Sky Lounge sang trọng, phòng tập gym 24/7, nhà hàng ẩm thực Á-Âu. Giá phòng từ 1.850.000 VNĐ/đêm. Số điện thoại lễ tân: 028 3822 5555.', '{"hotel_id":1,"city":"TP. Hồ Chí Minh","category":"HOTEL_INFO"}');

        -- Document 3: Sunrise Resort Phú Quốc
        INSERT INTO ai_knowledge_documents (id, title, category, source_type, hotel_id, status)
        VALUES (3, 'Sunrise Resort Phú Quốc - Kiên Giang', 'HOTEL_INFO', 'SYSTEM', 2, 'INDEXED');

        INSERT INTO ai_knowledge_chunks (document_id, chunk_index, content, metadata_json)
        VALUES (3, 0, 'Sunrise Resort Phú Quốc toạ lạc tại Bãi Trường, Xã Dương Tơ, TP. Phú Quốc, tỉnh Kiên Giang. Khu nghỉ dưỡng 5 sao sát bờ biển cát trắng với khuôn viên nhiệt đới xanh mát, hồ bơi vô cực ngắm hoàng hôn biển ngọc, spa trị liệu cao cấp, bãi tắm riêng và các hoạt động chèo thuyền kayak. Giá phòng từ 2.500.000 VNĐ/đêm. Hotline lễ tân: 0297 3988 888.', '{"hotel_id":2,"city":"Phú Quốc","category":"HOTEL_INFO"}');

        -- Document 4: The Lumiere Hotel Đà Nẵng
        INSERT INTO ai_knowledge_documents (id, title, category, source_type, hotel_id, status)
        VALUES (4, 'Khách sạn The Lumiere Hotel - Đà Nẵng', 'HOTEL_INFO', 'SYSTEM', 3, 'INDEXED');

        INSERT INTO ai_knowledge_chunks (document_id, chunk_index, content, metadata_json)
        VALUES (4, 0, 'The Lumiere Hotel toạ lạc tại 88 Bạch Đằng, Quận Hải Châu, TP. Đà Nẵng. Khách sạn 4 sao nằm ngay bờ sông Hàn với tầm nhìn trực diện ngắm Cầu Rồng phun lửa vào cuối tuần. Thiết kế phong cách hiện đại với Sky Bar tầng 20, hồ bơi vô cực nhìn ra sông Hàn, phòng hội nghị đa năng. Giá phòng từ 1.650.000 VNĐ/đêm. Số điện thoại: 0236 3844 666.', '{"hotel_id":3,"city":"Đà Nẵng","category":"HOTEL_INFO"}');

        -- Document 5: Melia Vinpearl Nha Trang
        INSERT INTO ai_knowledge_documents (id, title, category, source_type, hotel_id, status)
        VALUES (5, 'Melia Vinpearl Nha Trang - Khánh Hòa', 'HOTEL_INFO', 'SYSTEM', 4, 'INDEXED');

        INSERT INTO ai_knowledge_chunks (document_id, chunk_index, content, metadata_json)
        VALUES (5, 0, 'Melia Vinpearl Nha Trang nằm tại Đảo Hòn Tre, Phường Vĩnh Nguyên, TP. Nha Trang, tỉnh Khánh Hòa. Tổ hợp nghỉ dưỡng 5 sao cao cấp với bãi biển riêng biệt lập, cáp treo vượt biển, hệ thống nhà hàng hải sản thượng hạng, sân golf quốc tế 18 lỗ và khu vui chơi giải trí. Giá phòng từ 2.800.000 VNĐ/đêm. Số điện thoại: 0258 3598 999.', '{"hotel_id":4,"city":"Nha Trang","category":"HOTEL_INFO"}');

        -- Document 6: Thông tin các hạng phòng
        INSERT INTO ai_knowledge_documents (id, title, category, source_type, status)
        VALUES (6, 'Danh sách Hạng phòng và Tiện nghi chi tiết', 'ROOM_INFO', 'SYSTEM', 'INDEXED');

        INSERT INTO ai_knowledge_chunks (document_id, chunk_index, content, metadata_json)
        VALUES (6, 0, 'Tại Grand Vista Hotel (TP.HCM): 1) Deluxe King City View: Diện tích 38m2, 1 giường King, sức chứa 2 người lớn, bồn tắm nằm nhìn ra thành phố, giá 1.850.000 VNĐ/đêm. 2) Executive Suite: Diện tích 65m2, 1 giường King + sofa bed, sức chứa 3 khách, có phòng khách riêng, máy pha cà phê cao cấp, bồn Jacuzzi, đặc quyền vào Executive Lounge, giá 3.200.000 VNĐ/đêm.', '{"category":"ROOM_INFO","hotel_id":1}');

        INSERT INTO ai_knowledge_chunks (document_id, chunk_index, content, metadata_json)
        VALUES (6, 1, 'Tại Sunrise Resort Phú Quốc: 1) Ocean Front Bungalow: Diện tích 50m2, hiên tắm nắng sát bờ biển ngắm hoàng hôn, 1 giường King hướng biển, sức chứa 2 người, giá 2.500.000 VNĐ/đêm. 2) Family Beach Villa: Diện tích 110m2, 2 phòng ngủ (2 giường King), có hồ bơi mini riêng, bếp nhỏ, phòng khách rộng, sức chứa tối đa 4 người lớn, xe đưa đón sân bay miễn phí, giá 4.800.000 VNĐ/đêm.', '{"category":"ROOM_INFO","hotel_id":2}');

        INSERT INTO ai_knowledge_chunks (document_id, chunk_index, content, metadata_json)
        VALUES (6, 2, 'Tại The Lumiere Hotel Đà Nẵng: Premier River View diện tích 35m2, 1 giường Queen hoặc 2 giường đơn, sức chứa 2 người lớn, ban công rộng ngắm trọn vẹn sông Hàn và Cầu Rồng lung linh về đêm, giá 1.650.000 VNĐ/đêm. Tại Melia Vinpearl Nha Trang: Grand Ocean Suite diện tích 55m2, 1 giường King siêu lớn, bồn tắm kính ngắm bình minh trên vịnh Nha Trang, tặng rượu vang đón khách, giá 2.800.000 VNĐ/đêm.', '{"category":"ROOM_INFO"}');

        -- Document 7: Chính sách Nhận phòng và Trả phòng
        INSERT INTO ai_knowledge_documents (id, title, category, source_type, status)
        VALUES (7, 'Chính sách Nhận phòng (Check-in) và Trả phòng (Check-out)', 'POLICY', 'SYSTEM', 'INDEXED');

        INSERT INTO ai_knowledge_chunks (document_id, chunk_index, content, metadata_json)
        VALUES (7, 0, 'Quy định giờ giấc: Giờ nhận phòng (Check-in) tiêu chuẩn là từ 14:00. Giờ trả phòng (Check-out) tiêu chuẩn là trước 12:00 trưa. Thủ tục nhận phòng yêu cầu khách xuất trình Căn cước công dân (CCCD) hoặc Hộ chiếu (Passport) bản gốc còn hạn và mã đặt phòng (Booking ID). Khách nhận phòng sớm trước 6:00 sáng phụ thu 100% giá phòng đêm trước (kèm ăn sáng); nhận phòng sớm từ 6:00 đến 12:00 trưa phụ thu 50% tiền phòng 1 đêm. Khách trả phòng trễ từ 12:00 đến 18:00 phụ thu 50% tiền phòng 1 đêm; sau 18:00 phụ thu 100% giá phòng (tùy thuộc vào tình trạng phòng trống thực tế của khách sạn).', '{"category":"POLICY","topic":"checkin_checkout"}');

        -- Document 8: Chính sách Hủy phòng và Hoàn tiền
        INSERT INTO ai_knowledge_documents (id, title, category, source_type, status)
        VALUES (8, 'Chính sách Hủy phòng, Đổi ngày và Hoàn tiền', 'POLICY', 'SYSTEM', 'INDEXED');

        INSERT INTO ai_knowledge_chunks (document_id, chunk_index, content, metadata_json)
        VALUES (8, 0, 'Quy định hủy phòng tại HESTORA: 1) Hủy phòng miễn phí 100% nếu thực hiện trước ít nhất 48 giờ so với ngày nhận phòng (tính từ 14:00 ngày check-in). 2) Hủy phòng trong khoảng từ 24 giờ đến 48 giờ trước ngày nhận phòng: Phụ thu phí bằng 50% tổng đơn hoặc 1 đêm tiền phòng đầu tiên. 3) Hủy phòng trong vòng 24 giờ trước giờ nhận phòng hoặc khách không đến (No-show): Tính phí 100% tổng tiền đặt phòng, không hoàn tiền. 4) Tiền hoàn sẽ được chuyển về tài khoản thanh toán ban đầu của quý khách trong vòng 3 đến 7 ngày làm việc tùy ngân hàng phát hành thẻ.', '{"category":"POLICY","topic":"cancellation_refund"}');

        -- Document 9: Quy định Thanh toán
        INSERT INTO ai_knowledge_documents (id, title, category, source_type, status)
        VALUES (9, 'Hướng dẫn và Phương thức Thanh toán', 'PAYMENT', 'SYSTEM', 'INDEXED');

        INSERT INTO ai_knowledge_chunks (document_id, chunk_index, content, metadata_json)
        VALUES (9, 0, 'HESTORA hỗ trợ 3 phương thức thanh toán an toàn và tiện lợi: 1) Thanh toán khi nhận phòng (Cash on Arrival): Khách thanh toán bằng tiền mặt hoặc quẹt thẻ POS tại quầy lễ tân khi làm thủ tục check-in. 2) Thẻ tín dụng/ghi nợ quốc tế: Chấp nhận Visa, Mastercard, JCB qua cổng thanh toán bảo mật chuẩn PCI-DSS. 3) Cổng thanh toán trực tuyến: Hỗ trợ chuyển khoản quét mã VNPAY QR, OnePay nhanh chóng. Đối với các đơn đặt phòng vào dịp Lễ, Tết hoặc đặt từ 3 phòng trở lên, khách sạn yêu cầu đặt cọc trước tối thiểu 30% giá trị đơn hàng.', '{"category":"PAYMENT","topic":"methods"}');

        -- Document 10: FAQ - Câu hỏi thường gặp
        INSERT INTO ai_knowledge_documents (id, title, category, source_type, status)
        VALUES (10, 'Câu hỏi thường gặp (FAQ) và Tiện ích bổ sung', 'FAQ', 'SYSTEM', 'INDEXED');

        INSERT INTO ai_knowledge_chunks (document_id, chunk_index, content, metadata_json)
        VALUES (10, 0, 'FAQ 1 - Bữa sáng: Tất cả các giá phòng đặt trên website HESTORA đều đã bao gồm buffet sáng chuẩn quốc tế miễn phí phục vụ từ 6:30 đến 10:00 sáng hàng ngày. FAQ 2 - Đưa đón sân bay: Dịch vụ đưa đón sân bay miễn phí áp dụng cho khách đặt phòng hạng Villa hoặc Suite tại Sunrise Resort Phú Quốc và Melia Vinpearl Nha Trang. Đối với các hạng phòng khác, quý khách có thể liên hệ lễ tân trước 24 giờ để đăng ký xe riêng có tính phí ưu đãi.', '{"category":"FAQ","topic":"breakfast_shuttle"}');

        INSERT INTO ai_knowledge_chunks (document_id, chunk_index, content, metadata_json)
        VALUES (10, 1, 'FAQ 3 - Trẻ em & Giường phụ: Trẻ em dưới 6 tuổi được miễn phí ngủ chung giường với bố mẹ (tối đa 1 trẻ/phòng). Trẻ từ 6 đến 11 tuổi phụ thu ăn sáng 200.000 VNĐ/trẻ/ngày. Trẻ từ 12 tuổi trở lên tính như người lớn. Giường phụ (Extra Bed) có thể kê thêm tại một số hạng phòng với phụ phí 500.000 VNĐ/đêm kèm ăn sáng. FAQ 4 - Thú cưng: Khách sạn không cho phép mang theo thú cưng để đảm bảo không gian yên tĩnh và tránh dị ứng cho các khách lưu trú khác. FAQ 5 - Hóa đơn VAT: Hỗ trợ xuất hóa đơn điện tử VAT cho công ty, vui lòng cung cấp mã số thuế khi nhận phòng hoặc liên hệ lễ tân trong vòng 48h sau khi trả phòng.', '{"category":"FAQ","topic":"children_pet_invoice"}');

        PERFORM setval('ai_knowledge_documents_id_seq', (SELECT MAX(id) FROM ai_knowledge_documents));
        PERFORM setval('ai_knowledge_chunks_id_seq', (SELECT MAX(id) FROM ai_knowledge_chunks));
    END IF;
END $$;

-- ==================================================
-- 10. ROLE-BASED ACCESS CONTROL & STAFF ASSIGNMENT ENHANCEMENTS
-- ==================================================

-- 10.1 Allow 'CUSTOMER' role in users table
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;
ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role::text = ANY (ARRAY['USER'::character varying, 'CUSTOMER'::character varying, 'STAFF'::character varying, 'ADMIN'::character varying, 'MANAGER'::character varying]::text[]));

-- 10.2 Add assignment fields to ai_handoff_requests
ALTER TABLE ai_handoff_requests ADD COLUMN IF NOT EXISTS assigned_staff_id BIGINT REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE ai_handoff_requests ADD COLUMN IF NOT EXISTS assigned_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE ai_handoff_requests ADD COLUMN IF NOT EXISTS staff_reply TEXT;
CREATE INDEX IF NOT EXISTS idx_ai_handoff_assigned ON ai_handoff_requests(assigned_staff_id);

-- 10.3 Add assignment field to ai_conversations
ALTER TABLE ai_conversations ADD COLUMN IF NOT EXISTS assigned_staff_id BIGINT REFERENCES users(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_ai_conversations_assigned ON ai_conversations(assigned_staff_id);

-- 10.4 AI Configurations table
CREATE TABLE IF NOT EXISTS ai_configurations (
    config_key VARCHAR(100) PRIMARY KEY,
    config_value TEXT NOT NULL,
    description TEXT,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO ai_configurations (config_key, config_value, description)
VALUES 
    ('ai.similarity_threshold', '0.25', 'Ngưỡng tương đồng cosine tối thiểu cho tìm kiếm tri thức RAG'),
    ('ai.max_context_turns', '6', 'Số lượt tin nhắn lịch sử tối đa đưa vào ngữ cảnh hội thoại'),
    ('ai.service_status', 'HEALTHY', 'Trạng thái hoạt động của hệ thống AI Hotel Assistant'),
    ('ai.default_model', 'Dense-128d-HESTORA', 'Mô hình nhúng vector ngữ nghĩa tiếng Việt')
ON CONFLICT (config_key) DO NOTHING;

