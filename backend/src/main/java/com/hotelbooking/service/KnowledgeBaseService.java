package com.hotelbooking.service;

import com.hotelbooking.dto.KnowledgeDocumentDto;
import com.hotelbooking.model.AiKnowledgeChunk;
import com.hotelbooking.model.AiKnowledgeDocument;
import com.hotelbooking.model.Hotel;
import com.hotelbooking.model.Room;
import com.hotelbooking.repository.AiKnowledgeChunkRepository;
import com.hotelbooking.repository.AiKnowledgeDocumentRepository;
import com.hotelbooking.repository.HotelRepository;
import com.hotelbooking.repository.RoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class KnowledgeBaseService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseService.class);

    private final AiKnowledgeDocumentRepository docRepo;
    private final AiKnowledgeChunkRepository chunkRepo;
    private final HotelRepository hotelRepo;
    private final RoomRepository roomRepo;
    private final VectorStoreService vectorStoreService;

    public KnowledgeBaseService(
            AiKnowledgeDocumentRepository docRepo,
            AiKnowledgeChunkRepository chunkRepo,
            HotelRepository hotelRepo,
            RoomRepository roomRepo,
            VectorStoreService vectorStoreService) {
        this.docRepo = docRepo;
        this.chunkRepo = chunkRepo;
        this.hotelRepo = hotelRepo;
        this.roomRepo = roomRepo;
        this.vectorStoreService = vectorStoreService;
    }

    public List<KnowledgeDocumentDto> getAllDocuments() {
        List<AiKnowledgeDocument> docs = docRepo.findAllByOrderByCreatedAtDesc();
        List<KnowledgeDocumentDto> dtos = new ArrayList<>();
        for (AiKnowledgeDocument doc : docs) {
            KnowledgeDocumentDto dto = new KnowledgeDocumentDto();
            dto.setId(doc.getId());
            dto.setTitle(doc.getTitle());
            dto.setCategory(doc.getCategory());
            dto.setSourceType(doc.getSourceType());
            dto.setHotelId(doc.getHotelId());
            dto.setStatus(doc.getStatus());
            dto.setCreatedAt(doc.getCreatedAt());

            List<AiKnowledgeChunk> chunks = chunkRepo.findByDocumentId(doc.getId());
            dto.setChunkCount(chunks.size());
            if (!chunks.isEmpty()) {
                dto.setContent(chunks.get(0).getContent());
            }
            dtos.add(dto);
        }
        return dtos;
    }

    @Transactional
    public AiKnowledgeDocument addDocument(KnowledgeDocumentDto dto) {
        AiKnowledgeDocument doc = new AiKnowledgeDocument(
                dto.getTitle(),
                dto.getCategory(),
                dto.getSourceType() != null ? dto.getSourceType() : "MANUAL",
                dto.getHotelId()
        );
        AiKnowledgeDocument saved = docRepo.save(doc);

        if (dto.getContent() != null && !dto.getContent().isBlank()) {
            List<String> chunks = splitIntoChunks(dto.getContent(), 500);
            for (int i = 0; i < chunks.size(); i++) {
                AiKnowledgeChunk chunk = new AiKnowledgeChunk(
                        saved.getId(),
                        i,
                        chunks.get(i),
                        "{\"category\":\"" + dto.getCategory() + "\"}"
                );
                chunkRepo.save(chunk);
            }
        }

        vectorStoreService.refreshIndex();
        return saved;
    }

    @Transactional
    public boolean deleteDocument(Long id) {
        if (!docRepo.existsById(id)) return false;
        chunkRepo.deleteByDocumentId(id);
        docRepo.deleteById(id);
        vectorStoreService.refreshIndex();
        return true;
    }

    /**
     * Tự động đồng bộ thông tin khách sạn và phòng mới nhất từ Database vào Knowledge Base.
     */
    @Transactional
    public synchronized int syncFromLiveDatabase() {
        log.info("Starting knowledge base live sync from hotels and rooms tables...");
        int updated = 0;

        List<Hotel> hotels = hotelRepo.findAll();
        for (Hotel h : hotels) {
            String title = "Thông tin thực tế: " + h.getName();
            Optional<AiKnowledgeDocument> existing = docRepo.findAll().stream()
                    .filter(d -> h.getId().equals(d.getHotelId()) && "DATABASE_SYNC".equals(d.getSourceType()))
                    .findFirst();

            AiKnowledgeDocument doc = existing.orElseGet(() -> {
                AiKnowledgeDocument d = new AiKnowledgeDocument();
                d.setTitle(title);
                d.setCategory("HOTEL_INFO");
                d.setSourceType("DATABASE_SYNC");
                d.setHotelId(h.getId());
                return d;
            });
            doc.setUpdatedAt(LocalDateTime.now());
            AiKnowledgeDocument savedDoc = docRepo.save(doc);

            chunkRepo.deleteByDocumentId(savedDoc.getId());

            StringBuilder sb = new StringBuilder();
            sb.append("Khách sạn: ").append(h.getName()).append(". ");
            sb.append("Địa điểm: ").append(h.getLocation()).append(", ").append(h.getCity()).append(". ");
            if (h.getPrice() != null) {
                sb.append("Giá tham khảo từ: ").append(String.format("%,d", h.getPrice())).append(" VNĐ/đêm. ");
            }
            if (h.getRating() != null) {
                sb.append("Đánh giá: ").append(h.getRating()).append("/5.0 (").append(h.getReviewsCount()).append(" lượt đánh giá). ");
            }
            if (h.getDescription() != null) {
                sb.append("Mô tả: ").append(h.getDescription()).append(" ");
            }

            // Gắn danh sách phòng trực thuộc
            List<Room> rooms = roomRepo.findByHotelId(h.getId());
            if (!rooms.isEmpty()) {
                sb.append("Các hạng phòng hiện có: ");
                for (Room r : rooms) {
                    sb.append("[").append(r.getRoomType()).append(": ")
                            .append(String.format("%,d", r.getPricePerNight())).append(" VNĐ/đêm, ")
                            .append("sức chứa ").append(r.getCapacity()).append(" người, ")
                            .append("trạng thái ").append(r.getStatus()).append("] ");
                }
            }

            AiKnowledgeChunk chunk = new AiKnowledgeChunk(
                    savedDoc.getId(),
                    0,
                    sb.toString(),
                    "{\"hotel_id\":" + h.getId() + ",\"city\":\"" + h.getCity() + "\",\"sync\":true}"
            );
            chunkRepo.save(chunk);
            updated++;
        }

        vectorStoreService.refreshIndex();
        log.info("Knowledge base sync finished. Synced {} hotel documents.", updated);
        return updated;
    }

    private List<String> splitIntoChunks(String text, int maxChars) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) return chunks;

        String[] paragraphs = text.split("\n\n+");
        StringBuilder current = new StringBuilder();

        for (String p : paragraphs) {
            if (current.length() + p.length() > maxChars && current.length() > 0) {
                chunks.add(current.toString().trim());
                current = new StringBuilder();
            }
            current.append(p).append("\n\n");
        }
        if (current.length() > 0) {
            chunks.add(current.toString().trim());
        }
        return chunks;
    }
}

