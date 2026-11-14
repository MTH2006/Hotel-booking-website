package com.hotelbooking.repository;

import com.hotelbooking.model.AiHandoffRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiHandoffRequestRepository extends JpaRepository<AiHandoffRequest, Long> {
    List<AiHandoffRequest> findByStatusOrderByCreatedAtDesc(String status);
    List<AiHandoffRequest> findAllByOrderByCreatedAtDesc();
    List<AiHandoffRequest> findByAssignedStaffIdOrderByCreatedAtDesc(Long assignedStaffId);
    List<AiHandoffRequest> findByConversationId(Long conversationId);
}
