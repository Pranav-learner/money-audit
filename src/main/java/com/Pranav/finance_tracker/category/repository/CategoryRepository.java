package com.Pranav.finance_tracker.category.repository;

import com.Pranav.finance_tracker.expense.dto.CategoryDistributionResponse;
import com.Pranav.finance_tracker.category.entity.Category;
import com.Pranav.finance_tracker.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    List<Category> findByUserOrIsSystemTrue(User user);

    boolean existsByNameAndUser(String name, User user);

    Optional<Category> findByIdAndUser(UUID id, User user);



}
