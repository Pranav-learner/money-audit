package com.Pranav.finance_tracker.savings.controller;

import com.Pranav.finance_tracker.savings.dto.CreateSavingRequest;
import com.Pranav.finance_tracker.savings.dto.SavingResponse;
import com.Pranav.finance_tracker.savings.service.SavingService;
import com.Pranav.finance_tracker.user.entity.User;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/savings")
@RequiredArgsConstructor
public class SavingController {

    private final SavingService savingService;

    @PostMapping
    public ResponseEntity<SavingResponse> createSaving(
            @Valid @RequestBody CreateSavingRequest request,
            Authentication authentication
    ) {

        User user = (User) authentication.getPrincipal();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(savingService.createSaving(request, user));
    }

    @GetMapping
    public ResponseEntity<List<SavingResponse>> getSavings(
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(savingService.getAllSavings(user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SavingResponse> updateSaving(
            @PathVariable UUID id,
            @RequestBody CreateSavingRequest request,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(
                savingService.updateSaving(id, request, user)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSaving(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        savingService.deleteSaving(id, user);
        return ResponseEntity.noContent().build();
    }
}

