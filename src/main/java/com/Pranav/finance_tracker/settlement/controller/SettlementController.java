package com.Pranav.finance_tracker.settlement.controller;

import com.Pranav.finance_tracker.settlement.entity.Settlement;
import com.Pranav.finance_tracker.settlement.repository.SettlementRepository;
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
