package com.Pranav.finance_tracker.budget.repository;

import com.Pranav.finance_tracker.budget.entity.Budget;
import com.Pranav.finance_tracker.category.entity.Category;
import com.Pranav.finance_tracker.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, UUID> {

    Optional<Budget> findByUserAndCategoryAndMonthAndYear(
            User user,
            Category category,
            int month,
            int year
    );

    List<Budget> findByUserAndMonthAndYear(
            User user,
            int month,
            int year
    );
}
