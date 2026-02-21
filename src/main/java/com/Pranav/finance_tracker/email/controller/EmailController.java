package com.Pranav.finance_tracker.email.controller;

import com.Pranav.finance_tracker.auth.security.SecurityUtils;
import com.Pranav.finance_tracker.email.entity.EmailLog;
import com.Pranav.finance_tracker.email.repository.EmailLogRepository;
import com.Pranav.finance_tracker.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
public class EmailController {

    private final EmailLogRepository emailLogRepository;
    private final SecurityUtils securityUtils;

    @GetMapping("/history")
    public ResponseEntity<List<EmailLog>> getEmailHistory() {
        User currentUser = securityUtils.getCurrentUser();
        LocalDateTime fourteenDaysAgo = LocalDateTime.now().minusDays(14);
        return ResponseEntity.ok(emailLogRepository.findByUserAndSentAtAfterOrderBySentAtDesc(currentUser, fourteenDaysAgo));
    }
}
