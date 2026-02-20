package com.Pranav.finance_tracker.group.controller;

import com.Pranav.finance_tracker.group.entity.Settlement;
import com.Pranav.finance_tracker.group.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/settlements")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementRepository settlementRepository;

    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<Settlement>> getSettlementsByGroup(
            @PathVariable UUID groupId) {

        List<Settlement> settlements =
                settlementRepository.findByGroupId(groupId);
        return ResponseEntity.ok(settlements);
    }
}
