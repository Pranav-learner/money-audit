package com.Pranav.finance_tracker.payment.entity;

import com.Pranav.finance_tracker.group.entity.Group;
import com.Pranav.finance_tracker.payment.enums.PaymentMethod;
import com.Pranav.finance_tracker.payment.enums.PaymentStatus;
import com.Pranav.finance_tracker.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "from_user", nullable = false)
    private User fromUser;

    @ManyToOne
    @JoinColumn(name = "to_user", nullable = false)
    private User toUser;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @ManyToOne
    @JoinColumn(name = "group_id")
    private Group group;   // null → direct (1-to-1) payment

    private String note;

    private LocalDateTime createdAt;

    @Column(unique = true)
    private String requestId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PaymentStatus status;

    private String gatewayOrderId;
    private String gatewayPaymentId;

    @Column(length = 512)
    private String gatewaySignature;

    private LocalDateTime paidAt;

    @Column(nullable = false)
    private boolean isSettled = false;

    private LocalDateTime settledAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (method == null) method = PaymentMethod.MANUAL;
        if (status == null) status = PaymentStatus.SUCCESS;
        if (status == PaymentStatus.SUCCESS && paidAt == null) paidAt = createdAt;
    }
}
