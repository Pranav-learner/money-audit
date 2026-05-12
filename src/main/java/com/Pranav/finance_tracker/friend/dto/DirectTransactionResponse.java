package com.Pranav.finance_tracker.friend.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class DirectTransactionResponse {
    private UUID id;
    private String description;
    private BigDecimal amount;
    private UUID paidByUserId;
    private LocalDateTime date;
    private String type; // "EXPENSE" or "PAYMENT"
}
