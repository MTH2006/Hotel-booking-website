package com.hotelbooking.service;

import com.hotelbooking.dto.ChatMessageRequest;
import com.hotelbooking.dto.ChatMessageResponse;
import com.hotelbooking.model.*;
import com.hotelbooking.repository.AiHandoffRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ConversationService conversationService;
    private final RAGService ragService;
    private final HotelToolService hotelToolService;
    private final AiHandoffRequestRepository handoffRepo;

    @Value("${AI_SIMILARITY_THRESHOLD:0.25}")
    private double similarityThreshold;

    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{1,2})[/\\-\\.](\\d{1,2})(?:[/\\-\\.](\\d{4}))?");
    private static final Pattern GUEST_PATTERN = Pattern.compile("(\\d+)\\s*(?:người|khách|bạn|nguoi|guest)", Pattern.CASE_INSENSITIVE);
    private static final Pattern BOOKING_ID_PATTERN = Pattern.compile("(HB-\\d{5,8})", Pattern.CASE_INSENSITIVE);

    public ChatService(
            ConversationService conversationService,
            RAGService ragService,
            HotelToolService hotelToolService,
            AiHandoffRequestRepository handoffRepo) {
        this.conversationService = conversationService;
        this.ragService = ragService;
        this.hotelToolService = hotelToolService;
        this.handoffRepo = handoffRepo;
    }

    public ChatMessageResponse processMessage(ChatMessageRequest request, User authenticatedUser) {
        String userQuery = (request.getMessage() != null) ? request.getMessage().trim() : "";
        AiConversation conversation = conversationService.getOrCreateConversation(request.getSessionId(), authenticatedUser);

        // 1. Lưu tin nhắn của khách hàng
        conversationService.saveMessage(conversation.getId(), "USER", userQuery, "INCOMING", null);

        // 2. Lấy ngữ cảnh hội thoại gần đây để duy trì mạch đối thoại (Multi-turn Context)
        List<AiMessage> recentHistory = conversationService.getRecentContext(conversation.getId(), 6);
        String lastAssistantIntent = findLastAssistantIntent(recentHistory);

        // 3. Phân loại ý định (Intent Classification)
        String intent = classifyIntent(userQuery, lastAssistantIntent);

        ChatMessageResponse response = new ChatMessageResponse();
        response.setSessionId(conversation.getSessionId());
        response.setIntent(intent);

        // 4. Xử lý theo từng ý định nghiệp vụ hoặc RAG
        switch (intent) {
            case "OUT_OF_DOMAIN" -> handleOutOfDomain(response);
            case "HANDOFF" -> handleHandoff(userQuery, conversation, authenticatedUser, response);
            case "ROOM_SEARCH" -> handleRoomSearch(userQuery, recentHistory, response);
            case "PRICE_INQUIRY" -> handlePriceInquiry(userQuery, response);
            case "BOOKING_LOOKUP" -> handleBookingLookup(userQuery, authenticatedUser, response);
            case "BOOKING_CANCEL" -> handleBookingCancel(userQuery, authenticatedUser, response);
            default -> handleRAGOrFAQ(userQuery, recentHistory, response);
        }

        // 5. Lưu câu trả lời của AI vào hội thoại
        String citationsStr = (response.getCitations() != null && !response.getCitations().isEmpty())
                ? String.join("; ", response.getCitations()) : null;
        conversationService.saveMessage(conversation.getId(), "ASSISTANT", response.getReply(), response.getIntent(), citationsStr);

        return response;
    }

    private String classifyIntent(String query, String lastAssistantIntent) {
        String lower = query.toLowerCase(Locale.ROOT);
        String unaccented = EmbeddingService.stripAccents(lower);

        // Out-of-domain check
        if (isOutOfDomainQuery(query)) {
            return "OUT_OF_DOMAIN";
        }

        // Handoff to human
        if (unaccented.contains("gap nhan vien") || unaccented.contains("nhan vien ho tro")
                || unaccented.contains("tong dai") || unaccented.contains("nguoi that")
                || unaccented.contains("tu van vien") || unaccented.contains("cskh")) {
            return "HANDOFF";
        }

        // Policy inquiries about cancellation should be GENERAL_FAQ / POLICY, not BOOKING_CANCEL
        if (unaccented.contains("chinh sach huy") || unaccented.contains("dieu kien huy") || unaccented.contains("quy dinh huy") || unaccented.contains("chinh sach hoan tien")) {
            return "GENERAL_FAQ";
        }

        // Booking cancellation action
        if (unaccented.contains("huy phong") || unaccented.contains("huy don")
                || unaccented.contains("xac nhan huy") || (lower.contains("cancel") && lower.contains("booking"))) {
            return "BOOKING_CANCEL";
        }

        // Booking lookup
        if (BOOKING_ID_PATTERN.matcher(query).find() || unaccented.contains("kiem tra don")
                || unaccented.contains("don dat phong") || unaccented.contains("tra cuu don")
                || unaccented.contains("lich su dat phong") || unaccented.contains("thong tin dat phong")
                || unaccented.contains("kiem tra dat phong") || unaccented.contains("tra cuu dat phong")
                || unaccented.contains("xem dat phong") || unaccented.contains("kiem tra thong tin")) {
            return "BOOKING_LOOKUP";
        }

        // Price inquiry
        if (unaccented.contains("gia bao nhieu") || unaccented.contains("bao nhieu tien")
                || unaccented.contains("gia phong") || unaccented.contains("bang gia")) {
            return "PRICE_INQUIRY";
        }

        // Room search
        if (unaccented.contains("tim phong") || unaccented.contains("con phong")
                || unaccented.contains("dat phong") || unaccented.contains("cho 2 nguoi")
                || unaccented.contains("cho 4 nguoi") || unaccented.contains("phong doi")
                || unaccented.contains("phong don") || unaccented.contains("phong nghi")
                || "ROOM_SEARCH".equals(lastAssistantIntent)) {
            return "ROOM_SEARCH";
        }

        return "GENERAL_FAQ";
    }

    private boolean isOutOfDomainQuery(String query) {
        String unaccented = EmbeddingService.stripAccents(query.toLowerCase(Locale.ROOT));
        return unaccented.contains("ve may bay") || unaccented.contains("may bay")
                || unaccented.contains("vietnam airlines") || unaccented.contains("vietjet")
                || unaccented.contains("bamboo") || unaccented.contains("chuyen bay")
                || unaccented.contains("thoi tiet") || unaccented.contains("gia vang")
                || unaccented.contains("chung khoan") || unaccented.contains("bong da")
                || unaccented.contains("lap trinh") || unaccented.contains("viet code")
                || unaccented.contains("mua xe") || unaccented.contains("chinh tri");
    }

    private void handleOutOfDomain(ChatMessageResponse response) {
        response.setReply("Dạ em là trợ lý AI chuyên hỗ trợ các thông tin về khách sạn HESTORA. " +
                "Câu hỏi này nằm ngoài phạm vi hỗ trợ của em. Quý khách có thể hỏi em về đặt phòng, tiện ích, chính sách hoặc giá phòng nhé!");
        response.setSuggestions(List.of("Tìm phòng cho 2 người", "Chính sách hủy phòng", "Giờ nhận và trả phòng"));
    }

    private void handleRoomSearch(String query, List<AiMessage> history, ChatMessageResponse response) {
        String combined = combineHistoryWithQuery(history, query);

        String destination = null;
        if (combined.toLowerCase().contains("hồ chí minh") || combined.toLowerCase().contains("sài gòn") || combined.toLowerCase().contains("sg")) {
            destination = "Hồ Chí Minh";
        } else if (combined.toLowerCase().contains("phú quốc") || combined.toLowerCase().contains("phu quoc")) {
            destination = "Phú Quốc";
        } else if (combined.toLowerCase().contains("đà nẵng") || combined.toLowerCase().contains("da nang")) {
            destination = "Đà Nẵng";
        } else if (combined.toLowerCase().contains("nha trang")) {
            destination = "Nha Trang";
        }

        Integer guests = extractGuests(combined);
        List<LocalDate> dates = extractDates(combined);
        LocalDate checkIn = dates.size() > 0 ? dates.get(0) : LocalDate.now().plusDays(1);
        LocalDate checkOut = dates.size() > 1 ? dates.get(1) : checkIn.plusDays(1);

        Map<String, Object> searchResult = hotelToolService.searchAvailableRooms(destination, guests, checkIn, checkOut);
        response.setToolCalled("searchAvailableRooms");
        response.setToolResult(searchResult);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rooms = (List<Map<String, Object>>) searchResult.get("rooms");

        if (rooms == null || rooms.isEmpty()) {
            response.setReply("Dạ, hiện tại em chưa tìm thấy phòng trống phù hợp với yêu cầu của quý khách" +
                    (destination != null ? " tại " + destination : "") + " trong khoảng thời gian đã chọn. " +
                    "Quý khách có thể đổi ngày lưu trú hoặc điểm đến khác để em hỗ trợ kiểm tra thêm nhé!");
            response.setSuggestions(List.of("Tìm phòng tại Đà Nẵng", "Tìm phòng tại Phú Quốc", "Xem chính sách đặt phòng"));
            return;
        }

        StringBuilder reply = new StringBuilder();
        reply.append("Dạ, em đã kiểm tra hệ thống và tìm thấy ").append(rooms.size()).append(" hạng phòng khả dụng phù hợp với quý khách");
        if (destination != null) reply.append(" tại ").append(destination);
        reply.append(" (từ ngày ").append(checkIn.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .append(" đến ").append(checkOut.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("):\n\n");

        for (int i = 0; i < Math.min(3, rooms.size()); i++) {
            Map<String, Object> r = rooms.get(i);
            reply.append("🏨 **").append(r.get("hotelName")).append("** - *").append(r.get("city")).append("*\n")
                 .append("• Hạng phòng: **").append(r.get("roomType")).append("** (Sức chứa: ").append(r.get("capacity")).append(" khách)\n")
                 .append("• Giá: **").append(String.format("%,d", (Long) r.get("pricePerNight"))).append(" VNĐ/đêm**")
                 .append(" (Tổng ").append(r.get("nights")).append(" đêm: ").append(String.format("%,d", (Long) r.get("totalPrice"))).append(" VNĐ)\n")
                 .append("• Còn trống: ").append(r.get("availableRooms")).append(" phòng\n\n");
        }

        reply.append("Quý khách có thể nhấn trực tiếp vào thẻ phòng trên trang chủ hoặc để em hướng dẫn các bước đặt phòng ngay nhé!");
        response.setReply(reply.toString());
        response.setSuggestions(List.of("Hướng dẫn cách đặt phòng", "Chính sách hủy phòng thế nào?", "Các hình thức thanh toán"));
    }

    private void handlePriceInquiry(String query, ChatMessageResponse response) {
        Map<String, Object> priceResult = hotelToolService.queryRoomPrice(query, null, 1);
        response.setToolCalled("queryRoomPrice");
        response.setToolResult(priceResult);

        if (Boolean.TRUE.equals(priceResult.get("found"))) {
            response.setReply("Dạ, giá niêm yết của hạng phòng **" + priceResult.get("roomType") + "** tại " +
                    priceResult.get("hotelName") + " là **" +
                    String.format("%,d", (Long) priceResult.get("pricePerNight")) + " VNĐ/đêm** " +
                    "(sức chứa tiêu chuẩn: " + priceResult.get("capacity") + " khách).\n\n" +
                    "Giá phòng đã bao gồm buffet sáng miễn phí, tiện ích: " + priceResult.get("amenities") + ".");
            response.setSuggestions(List.of("Phòng này còn trống không?", "Chính sách hủy phòng", "Đặt phòng này ngay"));
        } else {
            handleRAGOrFAQ(query, Collections.emptyList(), response);
        }
    }

    private void handleBookingLookup(String query, User authenticatedUser, ChatMessageResponse response) {
        Matcher m = BOOKING_ID_PATTERN.matcher(query);
        String bookingId = m.find() ? m.group(1).toUpperCase() : null;

        Map<String, Object> lookupResult = hotelToolService.lookupUserBookings(authenticatedUser, bookingId);
        response.setToolCalled("lookupUserBookings");
        response.setToolResult(lookupResult);

        if (Boolean.FALSE.equals(lookupResult.get("authenticated"))) {
            response.setReply("🔒 Để bảo mật thông tin đặt phòng, quý khách vui lòng **Đăng nhập tài khoản** đã đặt trên website. " +
                    "Sau khi đăng nhập, em sẽ tra cứu và hiển thị toàn bộ chi tiết đơn đặt phòng ngay lập tức ạ!");
            response.setSuggestions(List.of("Đăng nhập tài khoản", "Hotline hỗ trợ CSKH", "Chính sách hủy phòng"));
            return;
        }

        if (Boolean.TRUE.equals(lookupResult.get("found"))) {
            if (lookupResult.containsKey("booking")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> b = (Map<String, Object>) lookupResult.get("booking");
                response.setReply("Dạ, em đã tìm thấy thông tin đơn đặt phòng **" + b.get("bookingId") + "** của quý khách:\n\n" +
                        "• Khách sạn: **" + b.get("hotelName") + "**\n" +
                        "• Hạng phòng: " + b.get("roomType") + "\n" +
                        "• Thời gian: Từ " + b.get("checkIn") + " đến " + b.get("checkOut") + "\n" +
                        "• Tổng thanh toán: **" + String.format("%,d", (Long) b.get("totalPrice")) + " VNĐ** (" + b.get("paymentStatus") + ")\n" +
                        "• Trạng thái đơn: **" + b.get("status") + "**\n\n" +
                        "Quý khách có cần hỗ trợ thêm thông tin gì về chuyến đi sắp tới không ạ?");
                response.setSuggestions(List.of("Chính sách hủy đơn này", "Giờ nhận phòng tiêu chuẩn", "Yêu cầu phòng tầng cao"));
            } else {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> list = (List<Map<String, Object>>) lookupResult.get("bookings");
                StringBuilder sb = new StringBuilder("Dạ, em tìm thấy ").append(list.size()).append(" đơn đặt phòng trong tài khoản của quý khách:\n\n");
                for (Map<String, Object> b : list) {
                    sb.append("• **").append(b.get("bookingId")).append("**: ").append(b.get("hotelName"))
                      .append(" (").append(b.get("checkIn")).append(") - Trạng thái: ").append(b.get("status")).append("\n");
                }
                response.setReply(sb.toString());
                response.setSuggestions(List.of("Xem chi tiết đơn mới nhất", "Chính sách nhận phòng", "Cần nhân viên hỗ trợ"));
            }
        } else {
            response.setReply((String) lookupResult.get("message"));
            response.setSuggestions(List.of("Tra cứu đơn khác", "Tìm phòng mới", "Gặp nhân viên hỗ trợ"));
        }
    }

    private void handleBookingCancel(String query, User authenticatedUser, ChatMessageResponse response) {
        Matcher m = BOOKING_ID_PATTERN.matcher(query);
        String bookingId = m.find() ? m.group(1).toUpperCase() : null;
        boolean isConfirmed = query.toLowerCase().contains("xac nhan huy") || query.toLowerCase().contains("xác nhận hủy");

        Map<String, Object> cancelResult = hotelToolService.cancelBookingSafely(authenticatedUser, bookingId, isConfirmed);
        response.setToolCalled("cancelBookingSafely");
        response.setToolResult(cancelResult);
        response.setReply((String) cancelResult.get("message"));

        if (Boolean.TRUE.equals(cancelResult.get("needsConfirmation"))) {
            response.setSuggestions(List.of("Xác nhận hủy " + cancelResult.get("bookingId"), "Không hủy nữa", "Xem lại chính sách hủy"));
        } else {
            response.setSuggestions(List.of("Kiểm tra đơn của tôi", "Tìm phòng khách sạn khác", "Liên hệ nhân viên"));
        }
    }

    private void handleHandoff(String query, AiConversation conversation, User user, ChatMessageResponse response) {
        response.setHandoffRecommended(true);

        AiHandoffRequest handoff = new AiHandoffRequest(
                conversation.getId(),
                user != null ? user.getName() : "Khách vãng lai",
                user != null ? user.getEmail() : null,
                user != null ? user.getPhone() : null,
                query
        );
        handoffRepo.save(handoff);

        response.setReply("Dạ, em đã ghi nhận yêu cầu hỗ trợ chuyên sâu của quý khách và chuyển tiếp đến bộ phận Lễ tân & Chăm sóc khách hàng HESTORA. " +
                "Nhân viên sẽ liên hệ trực tiếp qua số hotline hoặc email của quý khách trong ít phút.\n\n" +
                "📞 Hotline hỗ trợ khẩn cấp 24/7: **1900 8888** hoặc **028 3822 5555**.\n" +
                "Quý khách cũng có thể để lại số điện thoại ngay tại đây để nhân viên gọi lại hỗ trợ ngay nhé!");
        response.setSuggestions(List.of("Xem câu hỏi thường gặp", "Chính sách đặt phòng", "Quay lại trang chủ"));
    }

    private void handleRAGOrFAQ(String query, List<AiMessage> history, ChatMessageResponse response) {
        // Truy xuất kiến thức qua RAG Engine
        RAGService.RetrievalResult ragResult = ragService.retrieveContext(query, 3, similarityThreshold);

        if (!ragResult.isFound() || ragResult.getScoredChunks().isEmpty()) {
            response.setReply("Dạ, em rất tiếc hiện tại chưa có thông tin chính thức về câu hỏi này trong hệ thống dữ liệu khách sạn HESTORA. " +
                    "Để đảm bảo tính chính xác nhất cho chuyến đi của quý khách, em xin phép chuyển tiếp yêu cầu đến nhân viên lễ tân hỗ trợ hoặc quý khách có thể liên hệ tổng đài 24/7 qua số **1900 8888** nhé!");
            response.setHandoffRecommended(true);
            response.setSuggestions(List.of("Gặp nhân viên hỗ trợ", "Giờ nhận và trả phòng", "Chính sách hủy phòng"));
            return;
        }

        // Tạo câu trả lời tự nhiên từ ngữ cảnh được truy xuất (Grounded Synthesis)
        String groundedContext = ragResult.getCombinedContext();
        response.setCitations(ragResult.getCitations());

        StringBuilder reply = new StringBuilder();
        reply.append(synthesizeAnswer(query, groundedContext));

        response.setReply(reply.toString());
        response.setSuggestions(generateContextualSuggestions(query));
    }

    /**
     * Tổng hợp câu trả lời tự nhiên, thân thiện dựa trên đúng thông tin trích xuất được từ Knowledge Base.
     */
    private String synthesizeAnswer(String query, String context) {
        String unaccented = EmbeddingService.stripAccents(query.toLowerCase());

        if (unaccented.contains("nhan phong") || unaccented.contains("tra phong") || unaccented.contains("checkin") || unaccented.contains("checkout")) {
            return "Dạ, theo quy định tiêu chuẩn tại toàn bộ hệ thống HESTORA:\n" +
                   "• **Giờ nhận phòng (Check-in):** từ **14:00**.\n" +
                   "• **Giờ trả phòng (Check-out):** trước **12:00** trưa.\n\n" +
                   "Thủ tục nhận phòng cần xuất trình Căn cước công dân hoặc Hộ chiếu và mã đặt phòng (Booking ID). " +
                   "Nếu quý khách có nhu cầu nhận phòng sớm hoặc trả phòng muộn, khách sạn có hỗ trợ phụ thu linh hoạt tùy tình trạng phòng thực tế ạ!";
        }

        if (unaccented.contains("huy phong") || unaccented.contains("hoan tien") || unaccented.contains("chinh sach huy")) {
            return "Dạ, chính sách hủy phòng và hoàn tiền của HESTORA được áp dụng như sau:\n" +
                   "• **Hủy miễn phí 100%:** Khi thực hiện trước ít nhất **48 giờ** so với ngày nhận phòng.\n" +
                   "• **Hủy từ 24h - 48h:** Phụ thu phí 1 đêm tiền phòng đầu tiên.\n" +
                   "• **Hủy trong vòng 24h hoặc không đến (No-show):** Tính phí 100% chi phí đặt phòng.\n\n" +
                   "Tiền hoàn sẽ được chuyển về tài khoản ban đầu của quý khách trong 3 đến 7 ngày làm việc ạ.";
        }

        if (unaccented.contains("thanh toan") || unaccented.contains("chuyen khoan") || unaccented.contains("the") || unaccented.contains("tien mat")) {
            return "Dạ, HESTORA hỗ trợ các hình thức thanh toán đa dạng và bảo mật:\n" +
                   "1. **Thanh toán khi nhận phòng (Cash on Arrival):** Bằng tiền mặt hoặc thẻ tại quầy lễ tân.\n" +
                   "2. **Thẻ quốc tế:** Visa, Mastercard, JCB qua cổng bảo mật PCI-DSS.\n" +
                   "3. **Quét mã QR trực tuyến:** Chuyển khoản nhanh qua cổng VNPAY / OnePay.\n\n" +
                   "Lưu ý: Với đoàn từ 3 phòng trở lên hoặc các dịp Lễ, Tết, khách sạn yêu cầu đặt cọc trước tối thiểu 30% giá trị đơn hàng ạ.";
        }

        if (unaccented.contains("an sang") || unaccented.contains("buffet") || unaccented.contains("bua sang")) {
            return "Dạ vâng! Tất cả giá phòng đặt trực tiếp tại HESTORA đều **đã bao gồm bữa sáng buffet chuẩn quốc tế miễn phí** phục vụ hàng ngày từ **6:30 đến 10:00 sáng** với thực đơn phong phú Á - Âu và đặc sản địa phương ạ.";
        }

        if (unaccented.contains("san bay") || unaccented.contains("dua don")) {
            return "Dạ, dịch vụ xe đưa đón sân bay được **miễn phí** cho quý khách đặt phòng hạng Villa hoặc Suite tại Sunrise Resort Phú Quốc và Melia Vinpearl Nha Trang. Đối với các hạng phòng khác, quý khách vui lòng liên hệ lễ tân trước 24 giờ để đặt xe riêng với mức giá ưu đãi nhất nhé!";
        }

        if (unaccented.contains("tre em") || unaccented.contains("em be") || unaccented.contains("giuong phu")) {
            return "Dạ, chính sách trẻ em tại HESTORA như sau:\n" +
                   "• Trẻ em dưới 6 tuổi: **Miễn phí hoàn toàn** khi ngủ chung giường với bố mẹ (tối đa 1 trẻ/phòng).\n" +
                   "• Trẻ từ 6 đến 11 tuổi: Phụ thu ăn sáng 200.000 VNĐ/trẻ/ngày.\n" +
                   "• Trẻ từ 12 tuổi trở lên: Tính như người lớn.\n" +
                   "• Kê giường phụ (Extra bed): Phụ phí 500.000 VNĐ/đêm đã bao gồm bữa sáng.";
        }

        if (unaccented.contains("thu cung") || unaccented.contains("cho meo") || unaccented.contains("pet")) {
            return "Dạ, để đảm bảo không gian yên tĩnh, sạch sẽ và tránh dị ứng cho tất cả các khách lưu trú, hệ thống khách sạn HESTORA **chưa cho phép mang theo thú cưng** vào khuôn viên khách sạn. Rất mong quý khách thông cảm ạ!";
        }

        // Tóm tắt ngữ cảnh chuẩn xác từ RAG Chunks
        String[] chunks = context.split("\n\n");
        StringBuilder sb = new StringBuilder("Dạ, theo thông tin xác nhận từ hệ thống HESTORA:\n\n");
        for (String c : chunks) {
            String cleaned = c.replaceFirst("^\\[\\d+\\]\\s*", "").trim();
            if (!cleaned.isBlank()) {
                sb.append("• ").append(cleaned).append("\n\n");
            }
        }
        sb.append("Nếu quý khách cần thêm chi tiết nào khác, xin hãy cho em biết nhé!");
        return sb.toString();
    }

    private List<String> generateContextualSuggestions(String query) {
        String unaccented = EmbeddingService.stripAccents(query.toLowerCase());
        if (unaccented.contains("phong") || unaccented.contains("khach san")) {
            return List.of("Kiểm tra phòng còn trống", "Giờ nhận và trả phòng", "Chính sách hủy phòng");
        }
        return List.of("Tìm phòng cho 2 người", "Hướng dẫn đặt phòng", "Hình thức thanh toán");
    }

    private String findLastAssistantIntent(List<AiMessage> history) {
        for (int i = history.size() - 1; i >= 0; i--) {
            AiMessage msg = history.get(i);
            if ("ASSISTANT".equalsIgnoreCase(msg.getSenderType()) && msg.getIntent() != null) {
                return msg.getIntent();
            }
        }
        return "GENERAL_FAQ";
    }

    private String combineHistoryWithQuery(List<AiMessage> history, String query) {
        StringBuilder sb = new StringBuilder();
        for (AiMessage m : history) {
            sb.append(m.getContent()).append(" ");
        }
        sb.append(query);
        return sb.toString();
    }

    private Integer extractGuests(String text) {
        Matcher m = GUEST_PATTERN.matcher(text);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (Exception ignored) {}
        }
        if (text.toLowerCase().contains("phòng đôi") || text.toLowerCase().contains("2 người") || text.toLowerCase().contains("hai người")) {
            return 2;
        }
        if (text.toLowerCase().contains("phòng đơn") || text.toLowerCase().contains("1 người")) {
            return 1;
        }
        if (text.toLowerCase().contains("gia đình") || text.toLowerCase().contains("4 người")) {
            return 4;
        }
        return 2;
    }

    private List<LocalDate> extractDates(String text) {
        List<LocalDate> dates = new ArrayList<>();
        Matcher m = DATE_PATTERN.matcher(text);
        int currentYear = LocalDate.now().getYear();

        while (m.find()) {
            try {
                int day = Integer.parseInt(m.group(1));
                int month = Integer.parseInt(m.group(2));
                int year = (m.group(3) != null) ? Integer.parseInt(m.group(3)) : currentYear;
                if (day >= 1 && day <= 31 && month >= 1 && month <= 12) {
                    LocalDate d = LocalDate.of(year, month, day);
                    if (d.isBefore(LocalDate.now())) {
                        d = d.plusYears(1);
                    }
                    dates.add(d);
                }
            } catch (Exception ignored) {}
        }
        return dates;
    }
}

