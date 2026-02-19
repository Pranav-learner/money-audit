package com.Pranav.finance_tracker.budget.service;

import com.Pranav.finance_tracker.budget.dto.CreateBudgetRequest;
import com.Pranav.finance_tracker.budget.entity.Budget;
import com.Pranav.finance_tracker.category.entity.Category;
import com.Pranav.finance_tracker.category.repository.CategoryRepository;
import com.Pranav.finance_tracker.budget.repository.BudgetRepository;
import com.Pranav.finance_tracker.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;

    public void createOrUpdateBudget(CreateBudgetRequest request,
                                     User user) {

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow();

        Optional<Budget> existing =
                budgetRepository.findByUserAndCategoryAndMonthAndYear(
                        user,
                        category,
                        request.getMonth(),
                        request.getYear()
                );

        if (existing.isPresent()) {
            existing.get().setLimitAmount(request.getLimitAmount());
            budgetRepository.save(existing.get());
        } else {
            Budget budget = Budget.builder()
                    .limitAmount(request.getLimitAmount())
                    .month(request.getMonth())
                    .year(request.getYear())
                    .category(category)
                    .user(user)
                    .build();

            budgetRepository.save(budget);
        }
    }
}
