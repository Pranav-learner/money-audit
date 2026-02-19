package com.Pranav.finance_tracker.budget.entity;

import com.Pranav.finance_tracker.category.entity.Category;
import com.Pranav.finance_tracker.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(
        name = "budgets",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"user_id", "category_id", "month", "year"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private BigDecimal limitAmount;

    @Column(nullable = false)
    private int month;

    @Column(nullable = false)
    private int year;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
