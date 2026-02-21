package com.Pranav.finance_tracker.friend.entity;

import com.Pranav.finance_tracker.friend.enums.EditRequestStatus;
import com.Pranav.finance_tracker.group.entity.GroupExpense;
import com.Pranav.finance_tracker.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "edit_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EditRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "expense_id", nullable = false)
    private GroupExpense expense;

    @ManyToOne
    @JoinColumn(name = "requested_by", nullable = false)
    private User requestedBy;

    @ManyToOne
    @JoinColumn(name = "requested_to", nullable = false)
    private User requestedTo;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal newAmount;

    private String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EditRequestStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime resolvedAt;
}
