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
    private BigDecimal amount;       // total bill amount
    private BigDecimal myShare;      // current user's share
    private BigDecimal otherShare;   // other user's share
    private UUID paidByUserId;
    private LocalDateTime date;
    private String type; // "EXPENSE" or "PAYMENT"
    private String friendName;       // name of the other participant
    private UUID friendId;           // id of the other participant
    private String receiptUrl;       // Cloudinary URL of the receipt if attached
}
