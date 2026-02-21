package com.Pranav.finance_tracker.group.entity;

import com.Pranav.finance_tracker.group.enums.SplitType;
import com.Pranav.finance_tracker.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "group_expenses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupExpense {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String title;

    @Column(nullable = false)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private LocalDate expenseDate;

    @ManyToOne
    @JoinColumn(name = "paid_by", nullable = false)
    private User paidBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SplitType splitType;

    @ManyToOne
    @JoinColumn(name = "group_id")
    private Group group;   // null → direct (1-to-1) expense

    @ManyToOne
    @JoinColumn(name = "other_user_id")
    private User otherUser;  // only for direct expenses — the non-payer participant

    private LocalDateTime createdAt;
}

