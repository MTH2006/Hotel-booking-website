package com.hotelbooking.repository;

import com.hotelbooking.model.AiConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiConversationRepository extends JpaRepository<AiConversation, Long> {
    Optional<AiConversation> findBySessionId(String sessionId);
    List<AiConversation> findByUserIdOrderByUpdatedAtDesc(Long userId);
    List<AiConversation> findByAssignedStaffIdOrderByUpdatedAtDesc(Long assignedStaffId);
    List<AiConversation> findAllByOrderByUpdatedAtDesc();
}
