package com.Pranav.finance_tracker.friend.dto;

import com.Pranav.finance_tracker.group.enums.SplitType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class CreateDirectExpenseRequest {
    private String otherUserPhone;
    private java.util.UUID friendId;
    private java.util.UUID paidByUserId;

    @NotBlank(message = "Title is required")
    private String title;

    @NotNull(message = "Total amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal totalAmount;

    @NotNull(message = "Expense date is required")
    private LocalDate expenseDate;

    @NotNull(message = "Split type is required")
    private SplitType splitType;

    private BigDecimal myShare;        // for UNEQUAL split
    private BigDecimal otherShare;     // for UNEQUAL split
    private BigDecimal myPercentage;   // for PERCENTAGE split
    private BigDecimal otherPercentage; // for PERCENTAGE split
}
