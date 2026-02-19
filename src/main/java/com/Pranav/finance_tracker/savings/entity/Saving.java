package com.Pranav.finance_tracker.savings.entity;

import com.Pranav.finance_tracker.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.cglib.core.Local;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "savings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Saving {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String title;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate savingDate;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private LocalDateTime createdAt;
}

