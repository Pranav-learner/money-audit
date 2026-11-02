package com.Pranav.finance_tracker.group.dto;

import com.Pranav.finance_tracker.group.enums.SplitType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class CreateGroupExpenseRequest {
    private UUID groupId;
    private UUID otherUserId; // For direct expenses

    @NotBlank(message = "Title is required")
    private String title;

    @NotNull(message = "Total amount is required")
    @Positive(message = "Total amount must be positive")
    @DecimalMin(value = "0.01", message = "Total amount must be at least 0.01")
    private BigDecimal totalAmount;

    private LocalDate expenseDate;

    @NotNull(message = "Split type is required")
    private SplitType splitType;

    private List<SplitDetail> splits;
    private UUID categoryId;
    private UUID paidById;
    private String receiptUrl;
}
