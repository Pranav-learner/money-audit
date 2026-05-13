package com.Pranav.finance_tracker.receipt.dto;

import com.Pranav.finance_tracker.group.dto.SplitDetail;
import com.Pranav.finance_tracker.group.enums.SplitType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class ConfirmGroupReceiptRequest {

    private String title;

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotNull
    private LocalDate expenseDate;

    private String description;

    private UUID categoryId;

    private UUID groupId;     // For group splits
    private UUID otherUserId; // For direct (1-to-1) splits

    @NotNull
    private SplitType splitType;

    /**
     * Optional for EQUAL split (will auto-calculate for all group members if empty).
     * Required for UNEQUAL or PERCENTAGE splits.
     */
    private List<SplitDetail> splits;
}
