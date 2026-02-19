package com.Pranav.finance_tracker.category.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCategoryRequest {

    @NotBlank
    private String name;
}
