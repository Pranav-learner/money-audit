package com.Pranav.finance_tracker.email.repository;

import com.Pranav.finance_tracker.email.entity.EmailLog;
import com.Pranav.finance_tracker.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, UUID> {
    List<EmailLog> findByUserAndSentAtAfterOrderBySentAtDesc(User user, LocalDateTime date);
    
    void deleteBySentAtBefore(LocalDateTime date);
}
