package com.Pranav.finance_tracker.auth.security;

import com.Pranav.finance_tracker.auth.dto.LoginRequest;
import com.Pranav.finance_tracker.auth.service.AuthService;
import com.Pranav.finance_tracker.category.entity.Category;
import com.Pranav.finance_tracker.category.repository.CategoryRepository;
import com.Pranav.finance_tracker.category.service.CategoryService;
import com.Pranav.finance_tracker.exception.UnauthorizedException;
import com.Pranav.finance_tracker.user.entity.User;
import com.Pranav.finance_tracker.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityAndValidationTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private SecurityUtils mockSecurityUtils;

    @InjectMocks
    private AuthService authService;

    @InjectMocks
    private CategoryService categoryService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("SecurityUtils throws UnauthorizedException when user is anonymous")
    void securityUtilsRejectsAnonymous() {
        SecurityUtils securityUtils = new SecurityUtils(userRepository);
        Authentication anon = new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
        SecurityContextHolder.getContext().setAuthentication(anon);

        assertThatThrownBy(securityUtils::getCurrentUser)
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Full authentication is required");
    }

    @Test
    @DisplayName("AuthService throws BadCredentialsException (not 500) when user not found")
    void authServiceRejectsUnknownEmail() {
        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        LoginRequest req = new LoginRequest();
        req.setEmail("unknown@test.com");
        req.setPassword("Password123!");

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    @DisplayName("AuthService throws BadCredentialsException (not 500) when password is wrong")
    void authServiceRejectsWrongPassword() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("alice@test.com")
                .password("encoded_pass")
                .build();

        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong_pass", "encoded_pass")).thenReturn(false);

        LoginRequest req = new LoginRequest();
        req.setEmail("alice@test.com");
        req.setPassword("wrong_pass");

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    @DisplayName("CategoryService throws AccessDeniedException when deleting another user's category")
    void cannotDeleteAnotherUsersCategory() {
        User alice = User.builder().id(UUID.randomUUID()).name("Alice").build();
        User bob = User.builder().id(UUID.randomUUID()).name("Bob").build();

        when(mockSecurityUtils.getCurrentUser()).thenReturn(alice);

        UUID catId = UUID.randomUUID();
        Category bobsCategory = Category.builder()
                .id(catId)
                .name("Bob's Category")
                .user(bob)
                .isSystem(false)
                .build();

        when(categoryRepository.findById(catId)).thenReturn(Optional.of(bobsCategory));

        assertThatThrownBy(() -> categoryService.deleteCategory(catId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Not allowed to delete another user's category");
    }
}
