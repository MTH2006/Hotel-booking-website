
package com.hotelbooking.service;

import com.hotelbooking.model.CustomerNotification;
import com.hotelbooking.repository.CustomerNotificationRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CustomerNotificationService {

    private final CustomerNotificationRepository repository;

    public CustomerNotificationService(
            CustomerNotificationRepository repository) {
        this.repository = repository;
    }

    // Lay danh sach thong bao cua tai khoan
    @Transactional(readOnly = true)
    public List<CustomerNotification> getNotifications(Long userId) {

        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException(
                    "Nguoi dung khong hop le."
            );
        }

        return repository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    // Danh dau thong bao da doc
    @Transactional
    public Optional<CustomerNotification> markAsRead(
            Long notificationId,
            Long userId) {

        if (notificationId == null || notificationId <= 0
                || userId == null || userId <= 0) {
            throw new IllegalArgumentException(
                    "Thong tin khong hop le."
            );
        }

        Optional<CustomerNotification> found =
                repository.findById(notificationId);

        if (found.isEmpty()) {
            return Optional.empty();
        }

        CustomerNotification notification = found.get();

        // Khong cho phep doc thong bao cua nguoi khac
        if (!userId.equals(notification.getUserId())) {
            return Optional.empty();
        }

        notification.setRead(true);

        return Optional.of(repository.save(notification));
    }
}
