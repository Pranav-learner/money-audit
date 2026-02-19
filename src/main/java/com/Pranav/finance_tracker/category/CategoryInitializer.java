package com.Pranav.finance_tracker.category;

import com.Pranav.finance_tracker.category.entity.Category;
import com.Pranav.finance_tracker.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CategoryInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    @Override
    public void run(String... args) {

        if (categoryRepository.count() == 0) {

            List<String> defaults = List.of(
                    "Food",
                    "Transport",
                    "Bills",
                    "Shopping",
                    "Health",
                    "Entertainment"
            );

            defaults.forEach(name -> {
                categoryRepository.save(
                        Category.builder()
                                .name(name)
                                .isSystem(true)
                                .build()
                );
            });

            System.out.println("Default categories inserted.");
        }
    }
}
