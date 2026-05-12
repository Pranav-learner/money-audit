package com.Pranav.finance_tracker.receipt.entity;

import com.Pranav.finance_tracker.category.entity.Category;
import com.Pranav.finance_tracker.expense.entity.Expense;
import com.Pranav.finance_tracker.group.entity.Group;
import com.Pranav.finance_tracker.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "receipts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Receipt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "group_id")
    private Group group;

    @Column(nullable = false)
    private String storagePath;

    @Column(nullable = false)
    private String originalFilename;

    @Column(length = 50)
    private String contentType;

    @Column(columnDefinition = "TEXT")
    private String rawText;

    private String merchant;

    @Column(precision = 19, scale = 2)
    private BigDecimal extractedAmount;

    private LocalDate extractedDate;

    @ManyToOne
    @JoinColumn(name = "suggested_category_id")
    private Category suggestedCategory;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReceiptStatus status;

    @OneToOne
    @JoinColumn(name = "expense_id")
    private Expense linkedExpense;

    @OneToOne
    @JoinColumn(name = "group_expense_id")
    private com.Pranav.finance_tracker.group.entity.GroupExpense linkedGroupExpense;

    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status == null) status = ReceiptStatus.PARSED;
    }
}
