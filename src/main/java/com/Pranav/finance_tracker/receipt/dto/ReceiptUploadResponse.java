package com.Pranav.finance_tracker.receipt.dto;

import com.Pranav.finance_tracker.receipt.entity.ReceiptStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReceiptUploadResponse {
    private UUID receiptId;
    private String originalFilename;
    private ReceiptStatus status;
    private String merchant;
    private BigDecimal amount;
    private LocalDate date;
    private String suggestedCategoryName;
    private UUID suggestedCategoryId;
    private String rawText;
    private UUID linkedExpenseId;
    private UUID linkedGroupExpenseId;
}
