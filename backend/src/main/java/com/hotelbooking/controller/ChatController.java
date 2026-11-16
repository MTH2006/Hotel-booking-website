package com.hotelbooking.controller;

import com.hotelbooking.dto.*;
import com.hotelbooking.model.*;
import com.hotelbooking.repository.*;
import com.hotelbooking.security.AccessControlService;
import com.hotelbooking.service.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatService chatService;
    private final ConversationService conversationService;
    private final KnowledgeBaseService knowledgeBaseService;
    private final AccessControlService accessControl;
    private final AiHandoffRequestRepository handoffRepo;
    private final AiConversationRepository conversationRepo;
    private final AiMessageRepository messageRepo;
    private final AiKnowledgeChunkRepository chunkRepo;
    private final AiConfigurationRepository configRepo;

    public ChatController(
            ChatService chatService,
            ConversationService conversationService,
            KnowledgeBaseService knowledgeBaseService,
            AccessControlService accessControl,
            AiHandoffRequestRepository handoffRepo,
            AiConversationRepository conversationRepo,
            AiMessageRepository messageRepo,
            AiKnowledgeChunkRepository chunkRepo,
            AiConfigurationRepository configRepo) {
        this.chatService = chatService;
        this.conversationService = conversationService;
        this.knowledgeBaseService = knowledgeBaseService;
        this.accessControl = accessControl;
        this.handoffRepo = handoffRepo;
        this.conversationRepo = conversationRepo;
        this.messageRepo = messageRepo;
        this.chunkRepo = chunkRepo;
        this.configRepo = configRepo;
    }

    // =========================================================================
    // 1. CUSTOMER & PUBLIC CHATBOT ENDPOINTS
    // =========================================================================

    /**
     * Gửi tin nhắn trò chuyện với AI Hotel Assistant.
     * Mở cho CUSTOMER và khách vãng lai (Guest).
     */
    @PostMapping("/chat")
    public ResponseEntity<ChatMessageResponse> chat(
            @RequestBody ChatMessageRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        Optional<User> authenticatedUser = accessControl.authenticate(authorization);
        ChatMessageResponse response = chatService.processMessage(request, authenticatedUser.orElse(null));
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy lịch sử tin nhắn của một phiên hội thoại với kiểm tra quyền sở hữu dữ liệu (Data Ownership).
     * - CUSTOMER: Chỉ xem được hội thoại của chính mình.
     * - STAFF: Chỉ xem được hội thoại được phân công cho mình.
     * - ADMIN: Xem được toàn bộ hội thoại.
     */
    @GetMapping("/history/{sessionId}")
    public ResponseEntity<?> getHistory(
            @PathVariable String sessionId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        Optional<AiConversation> convOpt = conversationRepo.findBySessionId(sessionId);
        if (convOpt.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        AiConversation conv = convOpt.get();
        Optional<User> callerOpt = accessControl.authenticate(authorization);

        // Trường hợp 1: Hội thoại thuộc về một tài khoản đã đăng ký (user_id != null)
        if (conv.getUserId() != null) {
            if (callerOpt.isEmpty()) {
                // Khách vãng lai chưa đăng nhập không được xem lịch sử của người dùng đã đăng ký
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Vui lòng đăng nhập tài khoản sở hữu cuộc hội thoại để xem lịch sử."));
            }

            User caller = callerOpt.get();

            if (accessControl.isAdmin(caller)) {
                // ADMIN có quyền xem
                return ResponseEntity.ok(conversationService.getConversationMessages(conv.getId()));
            }

            if (accessControl.isStaff(caller)) {
                // STAFF chỉ xem nếu được phân công cuộc hội thoại hoặc có phiếu handoff được phân công
                boolean isAssigned = (conv.getAssignedStaffId() != null && conv.getAssignedStaffId().equals(caller.getId()));
                if (!isAssigned) {
                    List<AiHandoffRequest> handoffs = handoffRepo.findByConversationId(conv.getId());
                    isAssigned = handoffs.stream().anyMatch(h -> caller.getId().equals(h.getAssignedStaffId()));
                }
                if (!isAssigned) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("message", "Nhân viên chưa được phân công xử lý cuộc hội thoại này."));
                }
                return ResponseEntity.ok(conversationService.getConversationMessages(conv.getId()));
            }

            // CUSTOMER chỉ được xem nếu chính là chủ sở hữu
            if (!conv.getUserId().equals(caller.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Bạn không có quyền truy cập lịch sử cuộc hội thoại của khách hàng khác."));
            }

            return ResponseEntity.ok(conversationService.getConversationMessages(conv.getId()));
        }

        // Trường hợp 2: Hội thoại vãng lai (user_id == null)
        if (callerOpt.isPresent()) {
            User caller = callerOpt.get();
            // Nếu là CUSTOMER cố truy cập phiên vãng lai của người khác hoặc gán session
            if (accessControl.isCustomer(caller)) {
                // Tự động liên kết phiên vãng lai vào tài khoản của khách nếu khách đang đăng nhập
                conv.setUserId(caller.getId());
                conversationRepo.save(conv);
            }
        }

        return ResponseEntity.ok(conversationService.getConversationMessages(conv.getId()));
    }

    /**
     * Danh sách các cuộc trò chuyện của chính CUSTOMER đang đăng nhập.
     */
    @GetMapping("/customer/conversations")
    public ResponseEntity<?> getCustomerConversations(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        Optional<User> callerOpt = accessControl.authenticate(authorization);
        if (callerOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Chưa đăng nhập."));
        }

        User user = callerOpt.get();
        List<AiConversation> list = conversationRepo.findByUserIdOrderByUpdatedAtDesc(user.getId());
        return ResponseEntity.ok(list);
    }

    /**
     * Danh sách câu hỏi gợi ý nhanh ban đầu (Public).
     */
    @GetMapping("/suggestions")
    public ResponseEntity<List<String>> getSuggestions() {
        return ResponseEntity.ok(List.of(
                "Tìm phòng cho 2 người tại Đà Nẵng",
                "Khách sạn có dịch vụ đưa đón sân bay không?",
                "Giờ nhận và trả phòng là mấy giờ?",
                "Chính sách hủy phòng và hoàn tiền thế nào?",
                "Giá phòng Deluxe King bao nhiêu?",
                "Tôi muốn kiểm tra đơn đặt phòng của mình"
        ));
    }

    /**
     * Gửi yêu cầu chuyển tiếp sang nhân viên hỗ trợ trực tiếp (Human Handoff).
     */
    @PostMapping("/handoff")
    public ResponseEntity<Map<String, Object>> submitHandoff(
            @RequestBody HandoffRequestDto dto,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        Optional<User> caller = accessControl.authenticate(authorization);
        Optional<AiConversation> convOpt = conversationRepo.findBySessionId(dto.getSessionId());
        Long convId = convOpt.map(AiConversation::getId).orElse(null);

        String customerName = dto.getCustomerName();
        String customerEmail = dto.getCustomerEmail();
        String customerPhone = dto.getCustomerPhone();

        if (caller.isPresent()) {
            User u = caller.get();
            if (customerName == null || customerName.isBlank()) customerName = u.getName();
            if (customerEmail == null || customerEmail.isBlank()) customerEmail = u.getEmail();
            if (customerPhone == null || customerPhone.isBlank()) customerPhone = u.getPhone();
        }

        AiHandoffRequest req = new AiHandoffRequest(
                convId,
                customerName,
                customerEmail,
                customerPhone,
                dto.getQuestionSummary() != null ? dto.getQuestionSummary() : "Yêu cầu hỗ trợ từ khách hàng"
        );
        AiHandoffRequest saved = handoffRepo.save(req);

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("handoffId", saved.getId());
        res.put("message", "Yêu cầu của bạn đã được chuyển đến nhân viên lễ tân. Chúng tôi sẽ liên hệ trong ít phút.");
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    // =========================================================================
    // 2. STAFF SUPPORT ENDPOINTS (/api/ai/staff/**)
    // =========================================================================

    /**
     * Danh sách yêu cầu hỗ trợ (Human Handoff) cho STAFF.
     * STAFF xem được: phiếu PENDING (chưa nhận) và phiếu được phân công cho chính STAFF đó.
     */
    @GetMapping("/staff/handoffs")
    public ResponseEntity<?> getStaffHandoffs(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        Optional<User> userOpt = accessControl.authenticate(authorization);
        if (userOpt.isEmpty() || !accessControl.isStaffOrAdmin(userOpt.get())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Chỉ nhân viên hoặc quản trị viên mới có quyền truy cập."));
        }

        User staff = userOpt.get();
        List<AiHandoffRequest> all = handoffRepo.findAllByOrderByCreatedAtDesc();

        if (accessControl.isAdmin(staff)) {
            return ResponseEntity.ok(all);
        }

        // STAFF: Lấy các phiếu PENDING hoặc phiếu đã gán cho staff này
        List<AiHandoffRequest> filtered = all.stream()
                .filter(h -> "PENDING".equalsIgnoreCase(h.getStatus()) || staff.getId().equals(h.getAssignedStaffId()))
                .toList();

        return ResponseEntity.ok(filtered);
    }

    /**
     * STAFF tiếp nhận (Claim) một yêu cầu hỗ trợ.
     */
    @PostMapping("/staff/handoffs/{id}/claim")
    public ResponseEntity<?> claimHandoff(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        Optional<User> userOpt = accessControl.authenticate(authorization);
        if (userOpt.isEmpty() || !accessControl.isStaffOrAdmin(userOpt.get())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Chỉ nhân viên hoặc quản trị viên mới có quyền tiếp nhận yêu cầu."));
        }

        User staff = userOpt.get();
        return handoffRepo.findById(id).map(h -> {
            h.setAssignedStaffId(staff.getId());
            h.setAssignedAt(LocalDateTime.now());
            h.setStatus("IN_PROGRESS");
            handoffRepo.save(h);

            // Gán luôn nhân viên vào cuộc hội thoại liên kết
            if (h.getConversationId() != null) {
                conversationRepo.findById(h.getConversationId()).ifPresent(c -> {
                    c.setAssignedStaffId(staff.getId());
                    conversationRepo.save(c);
                });
            }

            Map<String, Object> res = new HashMap<>();
            res.put("success", true);
            res.put("message", "Đã tiếp nhận yêu cầu hỗ trợ thành công.");
            res.put("assignedStaffId", staff.getId());
            res.put("status", "IN_PROGRESS");
            return ResponseEntity.ok(res);
        }).orElse(ResponseEntity.notFound().build());
    }

    /**
     * STAFF gửi phản hồi giải đáp cho khách hàng.
     */
    @PostMapping("/staff/handoffs/{id}/reply")
    public ResponseEntity<?> replyHandoff(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        Optional<User> userOpt = accessControl.authenticate(authorization);
        if (userOpt.isEmpty() || !accessControl.isStaffOrAdmin(userOpt.get())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Chỉ nhân viên hoặc quản trị viên mới có quyền phản hồi."));
        }

        String replyMessage = body != null ? body.get("reply") : null;
        if (replyMessage == null || replyMessage.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Nội dung phản hồi không được để trống."));
        }

        User staff = userOpt.get();
        return handoffRepo.findById(id).map(h -> {
            h.setStaffReply(replyMessage);
            h.setResponseNote("Nhân viên " + staff.getName() + " đã phản hồi: " + replyMessage);
            handoffRepo.save(h);

            // Lưu tin nhắn nhân viên vào hội thoại
            if (h.getConversationId() != null) {
                conversationService.saveMessage(h.getConversationId(), "STAFF", "👨‍💼 [Nhân viên " + staff.getName() + "]: " + replyMessage, "STAFF_REPLY", null);
            }

            Map<String, Object> res = new HashMap<>();
            res.put("success", true);
            res.put("message", "Đã gửi phản hồi cho khách hàng thành công.");
            return ResponseEntity.ok(res);
        }).orElse(ResponseEntity.notFound().build());
    }

    /**
     * STAFF cập nhật trạng thái phiếu hỗ trợ (IN_PROGRESS, RESOLVED, CANCELLED).
     */
    @PutMapping("/staff/handoffs/{id}/status")
    public ResponseEntity<?> updateHandoffStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        Optional<User> userOpt = accessControl.authenticate(authorization);
        if (userOpt.isEmpty() || !accessControl.isStaffOrAdmin(userOpt.get())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Chỉ nhân viên hoặc quản trị viên mới có quyền cập nhật trạng thái."));
        }

        String newStatus = body != null ? body.get("status") : "RESOLVED";
        String note = body != null ? body.get("note") : null;

        return handoffRepo.findById(id).map(h -> {
            h.setStatus(newStatus != null ? newStatus.toUpperCase() : "RESOLVED");
            if ("RESOLVED".equalsIgnoreCase(h.getStatus())) {
                h.setResolvedAt(LocalDateTime.now());
            }
            if (note != null && !note.isBlank()) {
                h.setResponseNote(note);
            }
            handoffRepo.save(h);

            Map<String, Object> res = new HashMap<>();
            res.put("success", true);
            res.put("message", "Cập nhật trạng thái phiếu hỗ trợ thành công.");
            res.put("status", h.getStatus());
            return ResponseEntity.ok(res);
        }).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Xem các cuộc hội thoại được phân công cho STAFF.
     */
    @GetMapping("/staff/conversations")
    public ResponseEntity<?> getStaffAssignedConversations(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        Optional<User> userOpt = accessControl.authenticate(authorization);
        if (userOpt.isEmpty() || !accessControl.isStaffOrAdmin(userOpt.get())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Chỉ nhân viên hoặc quản trị viên mới có quyền truy cập."));
        }

        User staff = userOpt.get();
        if (accessControl.isAdmin(staff)) {
            return ResponseEntity.ok(conversationRepo.findAllByOrderByUpdatedAtDesc());
        }

        List<AiConversation> assigned = conversationRepo.findByAssignedStaffIdOrderByUpdatedAtDesc(staff.getId());
        return ResponseEntity.ok(assigned);
    }

    // =========================================================================
    // 3. ADMIN ONLY ENDPOINTS (/api/ai/admin/** & /api/ai/knowledge/**)
    // =========================================================================

    private boolean verifyAdminAccess(String authorization) {
        Optional<User> user = accessControl.authenticate(authorization);
        return user.isPresent() && accessControl.isAdmin(user.get());
    }

    @GetMapping("/admin/stats")
    public ResponseEntity<?> getAdminStats(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        if (!verifyAdminAccess(authorization)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Quyền truy cập bị từ chối: Chỉ Quản trị viên (ADMIN) mới có quyền truy cập thống kê AI."));
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalConversations", conversationRepo.count());
        stats.put("totalMessages", messageRepo.count());
        stats.put("totalDocuments", knowledgeBaseService.getAllDocuments().size());
        stats.put("totalChunks", chunkRepo.count());
        stats.put("pendingHandoffs", handoffRepo.findByStatusOrderByCreatedAtDesc("PENDING").size());
        stats.put("inProgressHandoffs", handoffRepo.findByStatusOrderByCreatedAtDesc("IN_PROGRESS").size());
        stats.put("resolvedHandoffs", handoffRepo.findByStatusOrderByCreatedAtDesc("RESOLVED").size());
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/admin/health")
    public ResponseEntity<?> getAdminAiHealth(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        if (!verifyAdminAccess(authorization)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Quyền truy cập bị từ chối: Yêu cầu quyền ADMIN."));
        }

        Map<String, Object> health = new LinkedHashMap<>();
        health.put("status", "HEALTHY");
        health.put("service", "HESTORA AI RAG Engine");
        health.put("vectorChunksLoaded", chunkRepo.count());
        health.put("embeddingModel", "Dense-128d-HESTORA");
        health.put("similaritySearch", "Cosine Similarity + Lexical Boost");
        health.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(health);
    }

    @GetMapping("/admin/config")
    public ResponseEntity<?> getAdminConfig(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        if (!verifyAdminAccess(authorization)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Quyền truy cập bị từ chối: Yêu cầu quyền ADMIN."));
        }

        return ResponseEntity.ok(configRepo.findAll());
    }

    @PutMapping("/admin/config")
    public ResponseEntity<?> updateAdminConfig(
            @RequestBody Map<String, String> updates,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        if (!verifyAdminAccess(authorization)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Quyền truy cập bị từ chối: Yêu cầu quyền ADMIN."));
        }

        if (updates != null) {
            updates.forEach((k, v) -> {
                Optional<AiConfiguration> item = configRepo.findByConfigKey(k);
                if (item.isPresent()) {
                    AiConfiguration cfg = item.get();
                    cfg.setConfigValue(v);
                    cfg.setUpdatedAt(LocalDateTime.now());
                    configRepo.save(cfg);
                } else {
                    configRepo.save(new AiConfiguration(k, v, "Cấu hình tùy chỉnh"));
                }
            });
        }

        return ResponseEntity.ok(Map.of("success", true, "message", "Cập nhật cấu hình AI thành công."));
    }

    @GetMapping({"/admin/documents", "/knowledge"})
    public ResponseEntity<?> getAdminDocuments(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        if (!verifyAdminAccess(authorization)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Quyền truy cập bị từ chối: Chỉ ADMIN mới được quản lý Knowledge Base."));
        }
        return ResponseEntity.ok(knowledgeBaseService.getAllDocuments());
    }

    @PostMapping({"/admin/documents", "/knowledge"})
    public ResponseEntity<?> addAdminDocument(
            @RequestBody KnowledgeDocumentDto dto,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        if (!verifyAdminAccess(authorization)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Quyền truy cập bị từ chối: Chỉ ADMIN mới được thêm Knowledge Base."));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(knowledgeBaseService.addDocument(dto));
    }

    @DeleteMapping({"/admin/documents/{id}", "/knowledge/{id}"})
    public ResponseEntity<?> deleteAdminDocument(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        if (!verifyAdminAccess(authorization)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Quyền truy cập bị từ chối: Chỉ ADMIN mới được xóa Knowledge Base."));
        }
        boolean deleted = knowledgeBaseService.deleteDocument(id);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @PostMapping("/admin/sync")
    public ResponseEntity<?> syncDatabaseKnowledge(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        if (!verifyAdminAccess(authorization)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Quyền truy cập bị từ chối: Chỉ ADMIN mới có quyền đồng bộ Vector Store."));
        }
        int synced = knowledgeBaseService.syncFromLiveDatabase();
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("syncedDocuments", synced);
        res.put("message", "Đồng bộ tri thức từ dữ liệu phòng vào Vector Store thành công.");
        return ResponseEntity.ok(res);
    }

    @GetMapping("/admin/conversations")
    public ResponseEntity<?> getAdminConversations(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        if (!verifyAdminAccess(authorization)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Quyền truy cập bị từ chối: Yêu cầu quyền ADMIN."));
        }
        return ResponseEntity.ok(conversationService.getAllConversations());
    }

    @GetMapping("/admin/handoffs")
    public ResponseEntity<?> getAdminHandoffs(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        if (!verifyAdminAccess(authorization)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Quyền truy cập bị từ chối: Yêu cầu quyền ADMIN."));
        }
        return ResponseEntity.ok(handoffRepo.findAllByOrderByCreatedAtDesc());
    }

    @PostMapping("/admin/handoffs/{id}/assign")
    public ResponseEntity<?> assignHandoffToStaff(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        if (!verifyAdminAccess(authorization)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Quyền truy cập bị từ chối: Yêu cầu quyền ADMIN."));
        }

        if (body == null || !body.containsKey("staffId")) {
            return ResponseEntity.badRequest().body(Map.of("message", "Thiếu thông tin staffId cần phân công."));
        }

        Long staffId = Long.valueOf(body.get("staffId").toString());
        return handoffRepo.findById(id).map(h -> {
            h.setAssignedStaffId(staffId);
            h.setAssignedAt(LocalDateTime.now());
            h.setStatus("IN_PROGRESS");
            handoffRepo.save(h);

            if (h.getConversationId() != null) {
                conversationRepo.findById(h.getConversationId()).ifPresent(c -> {
                    c.setAssignedStaffId(staffId);
                    conversationRepo.save(c);
                });
            }

            Map<String, Object> res = new HashMap<>();
            res.put("success", true);
            res.put("message", "Phân công phiếu hỗ trợ cho nhân viên (ID: " + staffId + ") thành công.");
            return ResponseEntity.ok(res);
        }).orElse(ResponseEntity.notFound().build());
    }
}
