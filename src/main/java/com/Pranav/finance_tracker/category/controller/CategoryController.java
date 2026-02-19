package com.Pranav.finance_tracker.category.controller;

import com.Pranav.finance_tracker.expense.PeriodType;
import com.Pranav.finance_tracker.expense.dto.CategoryDistributionResponse;
import com.Pranav.finance_tracker.category.dto.CategoryResponse;
import com.Pranav.finance_tracker.category.dto.CreateCategoryRequest;
import com.Pranav.finance_tracker.category.service.CategoryService;
import com.Pranav.finance_tracker.expense.service.ExpenseService;
import com.Pranav.finance_tracker.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final ExpenseService expenseService;

    @GetMapping
    public List<CategoryResponse> getCategories() {
        return categoryService.getAllCategories();
    }

    @PostMapping
    public CategoryResponse createCategory(
            @Valid @RequestBody CreateCategoryRequest request) {
        return categoryService.createCategory(request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable UUID id) {

        categoryService.deleteCategory(id);
        return new ResponseEntity<Void>( HttpStatus.NO_CONTENT);
    }


}
