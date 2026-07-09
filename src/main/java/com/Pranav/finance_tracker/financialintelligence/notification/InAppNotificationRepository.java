package com.Pranav.finance_tracker.financialintelligence.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Persistence for {@link InAppNotification}. Read endpoints for the notification feed
 * live outside this module's scope, but the query below is provided for convenience.
 */
@Repository
public interface InAppNotificationRepository extends JpaRepository<InAppNotification, UUID> {

    List<InAppNotification> findByUserIdAndReadFalseOrderByCreatedAtDesc(UUID userId);
}
