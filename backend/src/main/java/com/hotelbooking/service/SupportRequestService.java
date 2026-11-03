package com.hotelbooking.service;

import com.hotelbooking.model.SupportRequest;
import com.hotelbooking.repository.SupportRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

@Service
public class SupportRequestService {

    private final SupportRequestRepository supportRequestRepository;

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    public SupportRequestService(
            SupportRequestRepository supportRequestRepository) {
        this.supportRequestRepository = supportRequestRepository;
    }

    // FR-94: Tiep nhan yeu cau ho tro cua khach hang
    @Transactional
    public SupportRequest createSupportRequest(SupportRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Thong tin yeu cau ho tro khong hop le.");
        }

        String customerName =
                normalize(request.getCustomerName());

        String customerEmail =
                normalize(request.getCustomerEmail());

        String subject = normalize(request.getSubject());

        String message = normalize(request.getMessage());

        // Kiem tra ten khach hang
        if (customerName.isBlank() || customerName.length() > 150) {
            throw new IllegalArgumentException(
                    "Ten khach hang khong hop le.");
        }

        // Kiem tra email lien he
        if (customerEmail.length() > 255
                || !EMAIL_PATTERN.matcher(customerEmail).matches()) {
            throw new IllegalArgumentException(
                    "Email lien he khong hop le.");
        }

        // Kiem tra chu de
        if (subject.isBlank() || subject.length() > 200) {
            throw new IllegalArgumentException(
                    "Chu de ho tro khong hop le.");
        }

        // Kiem tra noi dung yeu cau
        if (message.isBlank() || message.length() > 5000) {
            throw new IllegalArgumentException(
                    "Noi dung ho tro khong hop le.");
        }

        request.setId(null);
        request.setCustomerName(customerName);
        request.setCustomerEmail(customerEmail);
        request.setSubject(subject);
        request.setMessage(message);

        // Trang thai va thoi gian do he thong quyet dinh
        request.setStatus("PENDING");
        request.setCreatedAt(LocalDateTime.now());

        // Luu yeu cau vao PostgreSQL
        return supportRequestRepository.save(request);
    }

    // Loai bo khoang trang thua va xu ly gia tri null
    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    // FR-95: Lay danh sach yeu cau ho tro moi nhat
    @Transactional(readOnly = true)
    public List<SupportRequest> getAllSupportRequests() {

        return supportRequestRepository.findAll(
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
    }

    // FR-95: Nhan vien tiep nhan yeu cau ho tro
    @Transactional
    public SupportRequest acceptSupportRequest(Long requestId) {

        // Kiem tra ma yeu cau
        if (requestId == null || requestId <= 0) {
            throw new IllegalArgumentException(
                    "Ma yeu cau ho tro khong hop le.");
        }

        // FR-95: Cap nhat co dieu kien de tranh tiep nhan trung
        int updated = supportRequestRepository
                .acceptIfPending(requestId);

        // Chi mot nhan vien co the tiep nhan thanh cong
        if (updated == 1) {

            // Lay lai yeu cau sau khi cap nhat PostgreSQL
            return supportRequestRepository.findById(requestId)
                    .orElseThrow(() -> new IllegalStateException(
                            "Khong the tai lai yeu cau da tiep nhan."));
        }

        // Kiem tra yeu cau co ton tai hay khong
        if (supportRequestRepository.findById(requestId).isEmpty()) {
            throw new IllegalArgumentException(
                    "Khong tim thay yeu cau ho tro.");
        }

        // Yeu cau khong con trang thai PENDING
        throw new IllegalStateException(
                "Yeu cau khong o trang thai cho xu ly.");
    }

    // FR-96: Nhan vien phan hoi va dong yeu cau ho tro
    @Transactional
    public SupportRequest resolveSupportRequest(Long requestId, String responseMessage) {
        if (requestId == null || requestId <= 0) {
            throw new IllegalArgumentException("Ma yeu cau ho tro khong hop le.");
        }

        SupportRequest req = supportRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay yeu cau ho tro."));

        req.setStatus("RESOLVED");
        if (responseMessage != null && !responseMessage.isBlank()) {
            req.setResponseMessage(responseMessage.trim());
        }
        req.setResolvedAt(LocalDateTime.now());
        return supportRequestRepository.save(req);
    }
}
