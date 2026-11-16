
package com.hotelbooking.repository;

import com.hotelbooking.model.SupportRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface SupportRequestRepository
        extends JpaRepository<SupportRequest, Long> {

    // FR-94: Luu yeu cau ho tro cua khach hang
    // FR-95: Truy xuat danh sach yeu cau ho tro
    
    // FR-95: Chi tiep nhan neu yeu cau van dang PENDING
    // Tra ve 1 neu cap nhat thanh cong, 0 neu khong cap nhat
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE SupportRequest s
            SET s.status = 'IN_PROGRESS'
            WHERE s.id = :requestId
              AND s.status = 'PENDING'
            """)
    int acceptIfPending(@Param("requestId") Long requestId);

}
