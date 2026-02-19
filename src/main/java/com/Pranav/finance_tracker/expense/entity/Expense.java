package com.Pranav.finance_tracker.expense.entity;

import com.Pranav.finance_tracker.category.entity.Category;
import com.Pranav.finance_tracker.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.cglib.core.Local;
import org.springframework.scheduling.quartz.LocalDataSourceJobStore;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "expenses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String title;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    private LocalDate expenseDate;

    private String description;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private LocalDateTime createdAt;
}
