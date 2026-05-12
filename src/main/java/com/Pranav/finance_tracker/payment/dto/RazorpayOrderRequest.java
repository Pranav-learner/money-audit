package com.Pranav.finance_tracker.payment.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class RazorpayOrderRequest {
    private BigDecimal amount;
    private UUID toUserId;
}
