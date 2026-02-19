package com.Pranav.finance_tracker.savings.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Builder
@Data
public class SavingResponse {

    private UUID id;
    private String title;
    private BigDecimal amount;
    private LocalDate savingDate;
}
