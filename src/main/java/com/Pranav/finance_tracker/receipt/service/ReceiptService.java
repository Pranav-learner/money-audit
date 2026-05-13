package com.Pranav.finance_tracker.receipt.service;

import com.Pranav.finance_tracker.auth.security.SecurityUtils;
import com.Pranav.finance_tracker.category.entity.Category;
import com.Pranav.finance_tracker.category.repository.CategoryRepository;
import com.Pranav.finance_tracker.exception.BadRequestException;
import com.Pranav.finance_tracker.exception.ResourceNotFoundException;
import com.Pranav.finance_tracker.expense.entity.Expense;
import com.Pranav.finance_tracker.expense.repository.ExpenseRepository;
import com.Pranav.finance_tracker.group.entity.Group;
import com.Pranav.finance_tracker.group.repository.GroupRepository;
import com.Pranav.finance_tracker.group.service.GroupExpenseService;
import com.Pranav.finance_tracker.group.dto.CreateGroupExpenseRequest;
import com.Pranav.finance_tracker.group.entity.GroupExpense;
import com.Pranav.finance_tracker.receipt.dto.ConfirmGroupReceiptRequest;
import com.Pranav.finance_tracker.receipt.dto.ConfirmReceiptRequest;
import com.Pranav.finance_tracker.receipt.dto.ReceiptParseResult;
import com.Pranav.finance_tracker.receipt.dto.ReceiptUploadResponse;
import com.Pranav.finance_tracker.receipt.entity.Receipt;
import com.Pranav.finance_tracker.receipt.entity.ReceiptStatus;
import com.Pranav.finance_tracker.receipt.repository.ReceiptRepository;
import com.Pranav.finance_tracker.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReceiptService {

    private final OcrService ocrService;
    private final ReceiptParserService parserService;
    private final MerchantCategoryService categoryRules;
    private final ReceiptRepository receiptRepository;
    private final ExpenseRepository expenseRepository;
    private final CategoryRepository categoryRepository;
    private final GroupRepository groupRepository;
    private final GroupExpenseService groupExpenseService;
    private final SecurityUtils securityUtils;
    private final Cloudinary cloudinary;

    @Value("${ocr.upload-dir}")
    private String uploadDir;

    @PostConstruct
    void init() throws IOException {
        Files.createDirectories(Paths.get(uploadDir));
    }

    @Transactional
    public ReceiptUploadResponse uploadAndParse(MultipartFile file, UUID groupId) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Receipt file is required");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BadRequestException("Only image uploads are supported (jpg, png)");
        }

        User user = securityUtils.getCurrentUser();

        Group group = null;
        if (groupId != null) {
            group = groupRepository.findById(groupId)
                    .orElseThrow(() -> new ResourceNotFoundException("Group not found: " + groupId));
        }

        File saved = persistToDisk(file);
        
        String cloudinaryUrl = saved.getAbsolutePath(); // Fallback
        try {
            Map uploadResult = cloudinary.uploader().upload(saved, ObjectUtils.emptyMap());
            cloudinaryUrl = uploadResult.get("secure_url").toString();
        } catch (IOException e) {
            log.error("Failed to upload receipt to Cloudinary", e);
            // Optionally, we can proceed with local path or fail.
        }

        Receipt receipt = Receipt.builder()
                .user(user)
                .group(group)
                .storagePath(cloudinaryUrl)
                .originalFilename(file.getOriginalFilename())
                .contentType(contentType)
                .status(ReceiptStatus.UPLOADED)
                .build();
        receipt = receiptRepository.save(receipt);

        ReceiptParseResult parsed;
        try {
            String text = ocrService.extractText(saved);
            parsed = parserService.parse(text);
            MerchantCategoryService.CategorySuggestion suggestion =
                    categoryRules.suggest(parsed.getMerchant(), parsed.getRawText(), user);

            receipt.setRawText(parsed.getRawText());
            receipt.setMerchant(parsed.getMerchant());
            receipt.setExtractedAmount(parsed.getAmount());
            receipt.setExtractedDate(parsed.getDate());
            receipt.setSuggestedCategory(suggestion.category());
            receipt.setStatus(ReceiptStatus.PARSED);
            if (suggestion.name() != null) parsed.setSuggestedCategoryName(suggestion.name());
        } catch (RuntimeException ex) {
            log.warn("OCR parsing failed for receipt {}: {}", receipt.getId(), ex.getMessage());
            receipt.setStatus(ReceiptStatus.PARSE_FAILED);
            parsed = ReceiptParseResult.builder().build();
        }

        receipt = receiptRepository.save(receipt);
        return toResponse(receipt, parsed);
    }

    @Transactional
    public ReceiptUploadResponse confirmAsExpense(UUID receiptId, ConfirmReceiptRequest request) {
        User user = securityUtils.getCurrentUser();

        Receipt receipt = receiptRepository.findByIdAndUser(receiptId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt not found: " + receiptId));

        if (receipt.getStatus() == ReceiptStatus.CONFIRMED) {
            throw new BadRequestException("Receipt already linked to an expense");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + request.getCategoryId()));

        Expense expense = Expense.builder()
                .title(request.getTitle() != null ? request.getTitle()
                        : (receipt.getMerchant() != null ? receipt.getMerchant() : "Receipt"))
                .amount(request.getAmount())
                .expenseDate(request.getExpenseDate() != null ? request.getExpenseDate() : LocalDate.now())
                .description(request.getDescription())
                .user(user)
                .category(category)
                .createdAt(LocalDateTime.now())
                .build();
        expense = expenseRepository.save(expense);

        receipt.setLinkedExpense(expense);
        receipt.setStatus(ReceiptStatus.CONFIRMED);
        receiptRepository.save(receipt);

        return getReceipt(receiptId);
    }

    @Transactional
    public ReceiptUploadResponse confirmAsGroupExpense(UUID receiptId, ConfirmGroupReceiptRequest request) {
        User user = securityUtils.getCurrentUser();

        Receipt receipt = receiptRepository.findByIdAndUser(receiptId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt not found: " + receiptId));

        if (receipt.getStatus() == ReceiptStatus.CONFIRMED) {
            throw new BadRequestException("Receipt already linked to an expense");
        }

        // Validate group exists and belongs to receipt or request
        UUID groupId = request.getGroupId();
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found: " + groupId));

        // Create GroupExpense via GroupExpenseService
        CreateGroupExpenseRequest groupReq = new CreateGroupExpenseRequest();
        groupReq.setGroupId(groupId);
        groupReq.setTitle(request.getTitle() != null ? request.getTitle() 
                : (receipt.getMerchant() != null ? receipt.getMerchant() : "Group Receipt"));
        groupReq.setTotalAmount(request.getAmount());
        groupReq.setExpenseDate(request.getExpenseDate() != null ? request.getExpenseDate() : LocalDate.now());
        groupReq.setSplitType(request.getSplitType());
        groupReq.setSplits(request.getSplits());
        groupReq.setOtherUserId(request.getOtherUserId());

        GroupExpense groupExpense = groupExpenseService.createGroupExpense(groupReq);

        receipt.setLinkedGroupExpense(groupExpense);
        receipt.setStatus(ReceiptStatus.CONFIRMED);
        receiptRepository.save(receipt);

        return getReceipt(receiptId);
    }

    public ReceiptUploadResponse getReceipt(UUID receiptId) {
        User user = securityUtils.getCurrentUser();
        Receipt receipt = receiptRepository.findByIdAndUser(receiptId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt not found: " + receiptId));
        return toResponse(receipt, fromEntity(receipt));
    }

    public List<ReceiptUploadResponse> listMyReceipts() {
        User user = securityUtils.getCurrentUser();
        return receiptRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(r -> toResponse(r, fromEntity(r)))
                .toList();
    }

    public List<ReceiptUploadResponse> listGroupReceipts(UUID groupId) {
        return receiptRepository.findByGroupIdOrderByCreatedAtDesc(groupId).stream()
                .map(r -> toResponse(r, fromEntity(r)))
                .toList();
    }

    private ReceiptParseResult fromEntity(Receipt r) {
        return ReceiptParseResult.builder()
                .merchant(r.getMerchant())
                .amount(r.getExtractedAmount())
                .date(r.getExtractedDate())
                .rawText(r.getRawText())
                .suggestedCategoryName(r.getSuggestedCategory() != null
                        ? r.getSuggestedCategory().getName() : null)
                .build();
    }

    private File persistToDisk(MultipartFile file) {
        try {
            String safeName = UUID.randomUUID() + "_" +
                    (file.getOriginalFilename() != null
                            ? file.getOriginalFilename().replaceAll("[^A-Za-z0-9._-]", "_")
                            : "upload");
            Path destination = Paths.get(uploadDir, safeName);
            file.transferTo(destination.toFile());
            return destination.toFile();
        } catch (IOException e) {
            log.error("Failed to persist uploaded receipt: {}", e.getMessage());
            throw new RuntimeException("Could not store uploaded file", e);
        }
    }

    private ReceiptUploadResponse toResponse(Receipt receipt, ReceiptParseResult parsed) {
        return ReceiptUploadResponse.builder()
                .receiptId(receipt.getId())
                .originalFilename(receipt.getOriginalFilename())
                .status(receipt.getStatus())
                .merchant(parsed.getMerchant())
                .amount(parsed.getAmount())
                .date(parsed.getDate())
                .suggestedCategoryName(parsed.getSuggestedCategoryName())
                .suggestedCategoryId(receipt.getSuggestedCategory() != null
                        ? receipt.getSuggestedCategory().getId() : null)
                .rawText(parsed.getRawText())
                .linkedExpenseId(receipt.getLinkedExpense() != null ? receipt.getLinkedExpense().getId() : null)
                .linkedGroupExpenseId(receipt.getLinkedGroupExpense() != null ? receipt.getLinkedGroupExpense().getId() : null)
                .receiptUrl(receipt.getStoragePath())
                .build();
    }
}
