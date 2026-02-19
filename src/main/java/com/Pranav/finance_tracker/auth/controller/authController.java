package com.Pranav.finance_tracker.auth.controller;

import com.Pranav.finance_tracker.auth.dto.LoginResponse;
import com.Pranav.finance_tracker.auth.dto.LoginRequest;
import com.Pranav.finance_tracker.auth.dto.RegisterRequest;
import com.Pranav.finance_tracker.auth.dto.RegisterResponse;
import com.Pranav.finance_tracker.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class authController {

    private final AuthService authService;

    @PostMapping("/register")
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request){
        return authService.register(request);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request){
        return authService.login(request);
    }
}
