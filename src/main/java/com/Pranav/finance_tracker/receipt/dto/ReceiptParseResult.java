package com.Pranav.finance_tracker.receipt.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReceiptParseResult {
    private String merchant;
    private BigDecimal amount;
    private LocalDate date;
    private String suggestedCategoryName;
    private String rawText;
}
