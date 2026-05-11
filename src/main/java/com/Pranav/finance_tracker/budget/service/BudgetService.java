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

    public java.util.List<com.Pranav.finance_tracker.budget.dto.BudgetResponse> getBudgets(User user, Integer month, Integer year) {
        int targetMonth = month != null ? month : java.time.LocalDate.now().getMonthValue();
        int targetYear = year != null ? year : java.time.LocalDate.now().getYear();

        java.util.List<Budget> budgets = budgetRepository.findByUserAndMonthAndYear(user, targetMonth, targetYear);
        
        return budgets.stream().map(b -> new com.Pranav.finance_tracker.budget.dto.BudgetResponse(
                b.getCategory().getId(),
                b.getLimitAmount(),
                b.getMonth(),
                b.getYear()
        )).toList();
    }
}
