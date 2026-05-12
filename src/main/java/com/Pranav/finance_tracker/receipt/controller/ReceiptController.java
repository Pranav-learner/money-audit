package com.Pranav.finance_tracker.receipt.controller;

import com.Pranav.finance_tracker.receipt.dto.ConfirmReceiptRequest;
import com.Pranav.finance_tracker.receipt.dto.ReceiptUploadResponse;
import com.Pranav.finance_tracker.receipt.service.ReceiptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/receipts")
@RequiredArgsConstructor
@Tag(name = "Receipts (OCR)", description = "Upload receipts, extract amount/date/merchant via OCR, auto-suggest categories")
public class ReceiptController {

    private final ReceiptService receiptService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a receipt image and run OCR")
    public ResponseEntity<ReceiptUploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "groupId", required = false) UUID groupId
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(receiptService.uploadAndParse(file, groupId));
    }

    @PostMapping("/{id}/confirm")
    @Operation(summary = "Confirm OCR result and create an individual Expense from this receipt")
    public ResponseEntity<ReceiptUploadResponse> confirm(
            @PathVariable("id") UUID id,
            @Valid @RequestBody ConfirmReceiptRequest request
    ) {
        return ResponseEntity.ok(receiptService.confirmAsExpense(id, request));
    }

    @PostMapping("/{id}/confirm-group")
    @Operation(summary = "Confirm OCR result and create a Group Expense (Split) from this receipt")
    public ResponseEntity<ReceiptUploadResponse> confirmGroup(
            @PathVariable("id") UUID id,
            @Valid @RequestBody com.Pranav.finance_tracker.receipt.dto.ConfirmGroupReceiptRequest request
    ) {
        return ResponseEntity.ok(receiptService.confirmAsGroupExpense(id, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a receipt by id")
    public ResponseEntity<ReceiptUploadResponse> get(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(receiptService.getReceipt(id));
    }

    @GetMapping
    @Operation(summary = "List all receipts for the current user")
    public ResponseEntity<List<ReceiptUploadResponse>> myReceipts() {
        return ResponseEntity.ok(receiptService.listMyReceipts());
    }

    @GetMapping("/group/{groupId}")
    @Operation(summary = "List all receipts uploaded to a group (useful before splitting)")
    public ResponseEntity<List<ReceiptUploadResponse>> byGroup(@PathVariable UUID groupId) {
        return ResponseEntity.ok(receiptService.listGroupReceipts(groupId));
    }
}
