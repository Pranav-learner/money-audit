package com.Pranav.finance_tracker.receipt.service;

import com.Pranav.finance_tracker.receipt.dto.ReceiptParseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class ReceiptParserService {

    private static final Pattern AMOUNT_LABELED = Pattern.compile(
            "(?i)(grand\\s*total|total\\s*amount|amount\\s*payable|amount\\s*due|net\\s*payable|net\\s*amount|payable|total)\\s*[:\\-]?\\s*(?:rs\\.?|inr|₹|\\$|chf|eur|usd|gbp|\\w{3})?\\s*([0-9]+(?:[.,][0-9]{1,2})?)",
            Pattern.MULTILINE);

    private static final Pattern AMOUNT_BARE = Pattern.compile(
            "(?:rs\\.?|inr|₹|\\$|chf|eur|usd|gbp)\\s*([0-9]+(?:[.,][0-9]{1,2})?)",
            Pattern.CASE_INSENSITIVE);

    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            buildFlexibleFormatter("dd/MM/yyyy"),
            buildFlexibleFormatter("dd-MM-yyyy"),
            buildFlexibleFormatter("dd.MM.yyyy"),
            buildFlexibleFormatter("yyyy-MM-dd"),
            buildFlexibleFormatter("yyyy/MM/dd"),
            buildFlexibleFormatter("dd MMM yyyy"),
            buildFlexibleFormatter("dd-MMM-yyyy"),
            buildFlexibleFormatter("MMM dd, yyyy"),
            buildFlexibleFormatter("MMM dd yyyy")
    );

    private static final Pattern DATE_PATTERN = Pattern.compile(
            "(\\d{1,2}[/.\\-]\\d{1,2}[/.\\-]\\d{2,4}" +
                    "|\\d{4}[/.\\-]\\d{1,2}[/.\\-]\\d{1,2}" +
                    "|\\d{1,2}\\s+[A-Za-z]{3,}\\s+\\d{2,4}" +
                    "|[A-Za-z]{3,}\\s+\\d{1,2},?\\s+\\d{2,4})");

    public ReceiptParseResult parse(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return ReceiptParseResult.builder().rawText(rawText).build();
        }
        
        log.info("--- RAW RECEIPT TEXT START ---\n{}\n--- RAW RECEIPT TEXT END ---", rawText);

        return ReceiptParseResult.builder()
                .merchant(extractMerchant(rawText))
                .amount(extractAmount(rawText))
                .date(extractDate(rawText))
                .rawText(rawText)
                .build();
    }

    private String extractMerchant(String text) {
        for (String line : text.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (trimmed.length() < 3) continue;
            if (trimmed.matches(".*\\d{3,}.*")) continue;
            if (trimmed.matches("(?i).*(invoice|receipt|bill|order|date|time|tax|gst).*")) continue;
            return trimmed;
        }
        return null;
    }

    private BigDecimal extractAmount(String text) {
        BigDecimal best = null;

        // 1. Labeled Total (Total: CHF 54.50)
        Matcher labeled = AMOUNT_LABELED.matcher(text);
        while (labeled.find()) {
            BigDecimal candidate = toAmount(labeled.group(2));
            if (candidate != null && (best == null || candidate.compareTo(best) > 0)) {
                best = candidate;
            }
        }
        if (best != null) return best;

        // 2. Amount with Currency (CHF 54.50 or 54.50 CHF)
        Pattern currencyPattern = Pattern.compile(
                "(?i)(?:rs\\.?|inr|₹|\\$|chf|eur|usd|gbp)\\s*([0-9]+(?:[.,][0-9]{1,2})?)|([0-9]+(?:[.,][0-9]{1,2})?)\\s*(?:rs\\.?|inr|₹|\\$|chf|eur|usd|gbp)");
        Matcher currencyMatcher = currencyPattern.matcher(text);
        while (currencyMatcher.find()) {
            String val = currencyMatcher.group(1) != null ? currencyMatcher.group(1) : currencyMatcher.group(2);
            BigDecimal candidate = toAmount(val);
            if (candidate != null && (best == null || candidate.compareTo(best) > 0)) {
                best = candidate;
            }
        }
        if (best != null) return best;

        // 3. Fallback: Largest number found that looks like an amount
        Pattern genericPattern = Pattern.compile("([0-9]+(?:[.,][0-9]{1,2}))");
        Matcher genericMatcher = genericPattern.matcher(text);
        while (genericMatcher.find()) {
            BigDecimal candidate = toAmount(genericMatcher.group(1));
            // Exclude dates (roughly) and very large numbers
            if (candidate != null && candidate.compareTo(BigDecimal.valueOf(1000000)) < 0) {
                if (best == null || candidate.compareTo(best) > 0) {
                    best = candidate;
                }
            }
        }

        return best;
    }

    private BigDecimal toAmount(String s) {
        if (s == null) return null;
        try {
            return new BigDecimal(s.replace(",", "."));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDate extractDate(String text) {
        Matcher matcher = DATE_PATTERN.matcher(text);
        while (matcher.find()) {
            String candidate = matcher.group(1);
            LocalDate parsed = tryParse(candidate);
            if (parsed != null) return parsed;
        }
        return null;
    }

    private LocalDate tryParse(String input) {
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(input.trim(), formatter);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static DateTimeFormatter buildFlexibleFormatter(String pattern) {
        return new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern(pattern)
                .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
                .toFormatter(Locale.ENGLISH);
    }
}
