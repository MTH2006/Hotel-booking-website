
package com.hotelbooking.repository;

import com.hotelbooking.model.CustomerNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerNotificationRepository
        extends JpaRepository<CustomerNotification, Long> {

    // Lay thong bao cua nguoi dung, moi nhat truoc
    List<CustomerNotification> findByUserIdOrderByCreatedAtDesc(
            Long userId
    );
}
