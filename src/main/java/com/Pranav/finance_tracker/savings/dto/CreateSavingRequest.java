package com.Pranav.finance_tracker.savings.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateSavingRequest {

    @NotBlank
    private String title;

    @NotNull
    private BigDecimal amount;

    @NotNull
    private LocalDate savingDate;
}
