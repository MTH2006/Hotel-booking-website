
package com.hotelbooking.repository;

import com.hotelbooking.model.Faq;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FaqRepository extends JpaRepository<Faq, Long> {

    // FR-96: Truy xuat, them, sua va xoa FAQ trong PostgreSQL

}
