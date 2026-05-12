package com.Pranav.finance_tracker.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateOrderResponse {
    private UUID paymentId;
    private String orderId;
    private String keyId;
    private BigDecimal amount;
    private String currency;
    private String status;
}
