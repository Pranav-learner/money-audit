package com.Pranav.finance_tracker.auth.controller;

import com.Pranav.finance_tracker.auth.dto.UserProfileResponse;
import com.Pranav.finance_tracker.auth.security.SecurityUtils;
import com.Pranav.finance_tracker.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserController {

    private final SecurityUtils securityUtils;

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser() {
        User user = securityUtils.getCurrentUser();
        
        UserProfileResponse response = new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone()
        );
        
        return ResponseEntity.ok(response);
    }
}
