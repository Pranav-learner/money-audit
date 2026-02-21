package com.Pranav.finance_tracker.friend.controller;

import com.Pranav.finance_tracker.friend.dto.CreateEditRequestDTO;
import com.Pranav.finance_tracker.friend.entity.EditRequest;
import com.Pranav.finance_tracker.friend.service.EditRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class EditRequestController {

    private final EditRequestService editRequestService;

    @PostMapping("/api/expenses/{expenseId}/edit-request")
    public ResponseEntity<String> createEditRequest(
            @PathVariable UUID expenseId,
            @RequestBody CreateEditRequestDTO dto) {
        return ResponseEntity.ok(editRequestService.createEditRequest(expenseId, dto));
    }

    @GetMapping("/api/edit-requests/pending")
    public ResponseEntity<List<EditRequest>> getPendingRequests() {
        return ResponseEntity.ok(editRequestService.getPendingRequests());
    }

    @PutMapping("/api/edit-requests/{id}/approve")
    public ResponseEntity<String> approve(@PathVariable UUID id) {
        return ResponseEntity.ok(editRequestService.approve(id));
    }

    @PutMapping("/api/edit-requests/{id}/reject")
    public ResponseEntity<String> reject(@PathVariable UUID id) {
        return ResponseEntity.ok(editRequestService.reject(id));
    }
}
