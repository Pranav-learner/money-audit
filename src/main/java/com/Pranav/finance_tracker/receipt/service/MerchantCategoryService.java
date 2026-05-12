package com.Pranav.finance_tracker.receipt.service;

import com.Pranav.finance_tracker.category.entity.Category;
import com.Pranav.finance_tracker.category.repository.CategoryRepository;
import com.Pranav.finance_tracker.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MerchantCategoryService {

    private final CategoryRepository categoryRepository;

    private static final Map<String, String> RULES = new LinkedHashMap<>();

    static {
        RULES.put("uber", "Transport");
        RULES.put("ola", "Transport");
        RULES.put("rapido", "Transport");
        RULES.put("indrive", "Transport");
        RULES.put("metro", "Transport");
        RULES.put("petrol", "Transport");
        RULES.put("fuel", "Transport");
        RULES.put("indianoil", "Transport");
        RULES.put("hp ", "Transport");
        RULES.put("bharat petroleum", "Transport");
        RULES.put("irctc", "Transport");

        RULES.put("zomato", "Food");
        RULES.put("swiggy", "Food");
        RULES.put("dominos", "Food");
        RULES.put("pizza", "Food");
        RULES.put("kfc", "Food");
        RULES.put("mcdonald", "Food");
        RULES.put("burger", "Food");
        RULES.put("cafe", "Food");
        RULES.put("restaurant", "Food");
        RULES.put("starbucks", "Food");
        RULES.put("haldiram", "Food");

        RULES.put("amazon", "Shopping");
        RULES.put("flipkart", "Shopping");
        RULES.put("myntra", "Shopping");
        RULES.put("ajio", "Shopping");
        RULES.put("meesho", "Shopping");
        RULES.put("nykaa", "Shopping");
        RULES.put("dmart", "Shopping");
        RULES.put("big bazaar", "Shopping");
        RULES.put("reliance fresh", "Shopping");

        RULES.put("apollo", "Health");
        RULES.put("medplus", "Health");
        RULES.put("pharmacy", "Health");
        RULES.put("hospital", "Health");
        RULES.put("clinic", "Health");
        RULES.put("1mg", "Health");
        RULES.put("pharmeasy", "Health");

        RULES.put("netflix", "Entertainment");
        RULES.put("hotstar", "Entertainment");
        RULES.put("prime video", "Entertainment");
        RULES.put("spotify", "Entertainment");
        RULES.put("bookmyshow", "Entertainment");
        RULES.put("pvr", "Entertainment");
        RULES.put("inox", "Entertainment");

        RULES.put("electricity", "Bills");
        RULES.put("airtel", "Bills");
        RULES.put("jio", "Bills");
        RULES.put("vi ", "Bills");
        RULES.put("bsnl", "Bills");
        RULES.put("gas bill", "Bills");
        RULES.put("water bill", "Bills");
        RULES.put("broadband", "Bills");
        RULES.put("recharge", "Bills");
    }

    public CategorySuggestion suggest(String merchant, String rawText, User user) {
        String haystack = ((merchant == null ? "" : merchant) + " " + (rawText == null ? "" : rawText))
                .toLowerCase(Locale.ROOT);

        for (Map.Entry<String, String> rule : RULES.entrySet()) {
            if (haystack.contains(rule.getKey())) {
                Optional<Category> match = findCategoryByName(rule.getValue(), user);
                if (match.isPresent()) {
                    log.debug("Auto-categorized merchant '{}' -> {}", merchant, rule.getValue());
                    return new CategorySuggestion(match.get(), rule.getValue());
                }
            }
        }
        return new CategorySuggestion(null, null);
    }

    private Optional<Category> findCategoryByName(String name, User user) {
        List<Category> candidates = categoryRepository.findByUserOrIsSystemTrue(user);
        return candidates.stream()
                .filter(c -> c.getName() != null && c.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    public record CategorySuggestion(Category category, String name) {}
}
