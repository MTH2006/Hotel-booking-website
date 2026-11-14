package com.hotelbooking.service;

import com.hotelbooking.model.AiConversation;
import com.hotelbooking.model.AiMessage;
import com.hotelbooking.model.User;
import com.hotelbooking.repository.AiConversationRepository;
import com.hotelbooking.repository.AiMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class ConversationService {

    private static final Logger log = LoggerFactory.getLogger(ConversationService.class);

    private final AiConversationRepository conversationRepo;
    private final AiMessageRepository messageRepo;

    public ConversationService(AiConversationRepository conversationRepo, AiMessageRepository messageRepo) {
        this.conversationRepo = conversationRepo;
        this.messageRepo = messageRepo;
    }

    @Transactional
    public AiConversation getOrCreateConversation(String sessionId, User user) {
        String cleanSessionId = (sessionId != null && !sessionId.isBlank()) ? sessionId.trim() : UUID.randomUUID().toString();
        Optional<AiConversation> existing = conversationRepo.findBySessionId(cleanSessionId);

        if (existing.isPresent()) {
            AiConversation conv = existing.get();
            if (user != null && conv.getUserId() == null) {
                conv.setUserId(user.getId());
                conversationRepo.save(conv);
            }
            return conv;
        }

        AiConversation newConv = new AiConversation(
                cleanSessionId,
                user != null ? user.getId() : null,
                "Trò chuyện " + cleanSessionId.substring(0, Math.min(8, cleanSessionId.length()))
        );
        return conversationRepo.save(newConv);
    }

    @Transactional
    public AiMessage saveMessage(Long conversationId, String senderType, String content, String intent, String citations) {
        AiMessage msg = new AiMessage(conversationId, senderType, content, intent, citations);
        AiMessage saved = messageRepo.save(msg);

        conversationRepo.findById(conversationId).ifPresent(c -> {
            c.setUpdatedAt(LocalDateTime.now());
            if ("USER".equalsIgnoreCase(senderType) && "Cuộc trò chuyện mới".equals(c.getTitle())) {
                String title = content.length() > 40 ? content.substring(0, 37) + "..." : content;
                c.setTitle(title);
            }
            conversationRepo.save(c);
        });

        return saved;
    }

    public List<AiMessage> getConversationMessages(Long conversationId) {
        return messageRepo.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }

    public List<AiMessage> getRecentContext(Long conversationId, int limit) {
        List<AiMessage> all = messageRepo.findByConversationIdOrderByCreatedAtAsc(conversationId);
        if (all.size() <= limit) {
            return all;
        }
        return all.subList(all.size() - limit, all.size());
    }

    public List<AiConversation> getAllConversations() {
        return conversationRepo.findAllByOrderByUpdatedAtDesc();
    }
}

