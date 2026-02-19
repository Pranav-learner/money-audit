package com.Pranav.finance_tracker.savings.service;

import com.Pranav.finance_tracker.savings.dto.CreateSavingRequest;
import com.Pranav.finance_tracker.savings.dto.SavingResponse;
import com.Pranav.finance_tracker.savings.entity.Saving;
import com.Pranav.finance_tracker.savings.repository.SavingRepository;
import com.Pranav.finance_tracker.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SavingService {

    private final SavingRepository savingRepository;

    public SavingResponse createSaving(CreateSavingRequest request, User user) {

        Saving saving = Saving.builder()
                .title(request.getTitle())
                .amount(request.getAmount())
                .savingDate(request.getSavingDate())
                .user(user)
                .createdAt(LocalDateTime.now())
                .build();

        savingRepository.save(saving);

        return mapToResponse(saving);
    }

    public List<SavingResponse> getAllSavings(User user) {
        return savingRepository.findByUser(user)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public SavingResponse updateSaving(UUID id,
                                       CreateSavingRequest request,
                                       User user) {

        Saving saving = savingRepository
                .findByIdAndUser(id, user)
                .orElseThrow(() -> new RuntimeException("Saving not found"));

        saving.setTitle(request.getTitle());
        saving.setAmount(request.getAmount());
        saving.setSavingDate(request.getSavingDate());

        savingRepository.save(saving);

        return mapToResponse(saving);
    }

    public void deleteSaving(UUID id, User user) {

        Saving saving = savingRepository
                .findByIdAndUser(id, user)
                .orElseThrow(() -> new RuntimeException("Saving not found"));

        savingRepository.delete(saving);
    }

    private SavingResponse mapToResponse(Saving saving) {
        return SavingResponse.builder()
                .id(saving.getId())
                .title(saving.getTitle())
                .amount(saving.getAmount())
                .savingDate(saving.getSavingDate())
                .build();
    }
}
