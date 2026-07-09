package com.Pranav.finance_tracker.financialintelligence.forecast.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A user-defined financial goal (e.g. "Laptop — ₹120,000 in 8 months").
 *
 * <p>The planning engine keeps the derived fields ({@code monthlyContributionRequired},
 * {@code projectedCompletionDate}) up to date. References its owner by {@code userId} only.</p>
 */
@Entity
@Table(
        name = "financial_goals",
        indexes = {
                @Index(name = "idx_goal_user_status", columnList = "user_id, status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 160)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "goal_type", nullable = false, length = 40)
    private GoalType goalType;

    @Column(name = "target_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal targetAmount;

    @Column(name = "current_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal currentAmount;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    /** Derived: the monthly saving needed to hit the target by {@link #targetDate}. */
    @Column(name = "monthly_contribution_required", precision = 15, scale = 2)
    private BigDecimal monthlyContributionRequired;

    /** Derived: when the goal is projected to complete at the user's current saving rate. */
    @Column(name = "projected_completion_date")
    private LocalDate projectedCompletionDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GoalStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
