package com.Pranav.finance_tracker.auth.service;

import com.Pranav.finance_tracker.auth.dto.LoginResponse;
import com.Pranav.finance_tracker.auth.dto.LoginRequest;
import com.Pranav.finance_tracker.auth.dto.RegisterRequest;
import com.Pranav.finance_tracker.auth.dto.RegisterResponse;
import com.Pranav.finance_tracker.auth.security.JwtService;
import com.Pranav.finance_tracker.exception.EmailAlreadyExistsException;
import com.Pranav.finance_tracker.user.entity.User;
import com.Pranav.finance_tracker.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.Pranav.finance_tracker.auth.dto.UserProfileResponse;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    // Registration
    public RegisterResponse register(RegisterRequest request){

        if(userRepository.existsByEmail(request.getEmail())){
            throw new EmailAlreadyExistsException(("Email already in use"));
        }

        User user = User.builder()
                     .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .isVerified(true)
                .build();

        userRepository.save(user);
        
        String token = jwtService.generateToken(
                user.getId(),
                user.getEmail()
        );

        UserProfileResponse profile = new UserProfileResponse(
                user.getId(), user.getName(), user.getEmail(), user.getPhone()
        );

        return new RegisterResponse(token, profile);
    }

    // login
    public LoginResponse login(LoginRequest request){
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(()-> new RuntimeException("user not found"));

        if(!passwordEncoder.matches(request.getPassword(), user.getPassword())){
            throw new RuntimeException("Invalid Password");
        }

        String token = jwtService.generateToken(
                user.getId(),
                user.getEmail()
        );

        UserProfileResponse profile = new UserProfileResponse(
                user.getId(), user.getName(), user.getEmail(), user.getPhone()
        );

        return new LoginResponse(token, profile);
    }
}
