package com.Pranav.finance_tracker.group.entity;

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
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    private String note;

    private LocalDateTime createdAt;

    @Column(unique = true)
    private String requestId;
}
