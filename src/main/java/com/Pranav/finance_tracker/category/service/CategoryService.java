package com.Pranav.finance_tracker.category.service;

import com.Pranav.finance_tracker.auth.security.SecurityUtils;
import com.Pranav.finance_tracker.expense.PeriodType;
import com.Pranav.finance_tracker.expense.dto.CategoryDistributionResponse;
import com.Pranav.finance_tracker.category.dto.CategoryResponse;
import com.Pranav.finance_tracker.category.dto.CreateCategoryRequest;
import com.Pranav.finance_tracker.category.entity.Category;
import com.Pranav.finance_tracker.category.repository.CategoryRepository;
import com.Pranav.finance_tracker.user.entity.User;
import com.Pranav.finance_tracker.user.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;

    public List<CategoryResponse> getAllCategories(){

        User user = securityUtils.getCurrentUser();



        return categoryRepository
                .findByUserOrIsSystemTrue(user)
                .stream()
                .map(this::mapToResponse)
                .toList();


    }




    public CategoryResponse createCategory(CreateCategoryRequest request) {

        User  user = securityUtils.getCurrentUser();



        if (categoryRepository.existsByNameAndUser(request.getName(), user)) {
            throw new RuntimeException("Category already exists");
        }

        Category category = Category.builder()
                .name(request.getName())
                .isSystem(false)
                .user(user)
                .build();

        categoryRepository.save(category);

        return mapToResponse(category);
    }

    public void deleteCategory(UUID id) {

        User  user = securityUtils.getCurrentUser();


        Category category = categoryRepository
                .findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        if (category.isSystem()) {  // System categories cannot be deleted
            throw new RuntimeException("System categories cannot be deleted");
        }

        if (!category.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Not allowed");
        }

        categoryRepository.delete(category);
    }



    private CategoryResponse mapToResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .isSystem(category.isSystem())
                .build();
    }

}
