package com.Pranav.finance_tracker.financialintelligence.notification;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A persisted in-app notification.
 *
 * <p>The Financial Intelligence Engine writes one of these after generating new insights.
 * It is intentionally <b>persist-only</b>: real-time delivery (WebSocket) and the frontend
 * bell/feed are expected to read from this table later. No email is ever sent from here.</p>
 */
@Entity
@Table(
        name = "in_app_notifications",
        indexes = {
                @Index(name = "idx_notification_user_created", columnList = "user_id, created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InAppNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 500)
    private String body;

    /** Logical notification type, e.g. {@code FINANCIAL_INSIGHT}. Lets the frontend route/filter. */
    @Column(nullable = false, length = 60)
    private String type;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
