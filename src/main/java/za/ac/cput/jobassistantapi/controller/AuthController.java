package za.ac.cput.jobassistantapi.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.jobassistantapi.dto.request.ChangePasswordRequest;
import za.ac.cput.jobassistantapi.dto.request.ForgotPasswordRequest;
import za.ac.cput.jobassistantapi.dto.request.GoogleLoginRequest;
import za.ac.cput.jobassistantapi.dto.request.LoginRequest;
import za.ac.cput.jobassistantapi.dto.request.RegisterRequest;
import za.ac.cput.jobassistantapi.dto.request.ResetPasswordRequest;
import za.ac.cput.jobassistantapi.dto.response.AuthResponse;
import za.ac.cput.jobassistantapi.exception.ResourceNotFoundException;
import za.ac.cput.jobassistantapi.model.User;
import za.ac.cput.jobassistantapi.repository.UserRepository;
import za.ac.cput.jobassistantapi.security.RateLimiterService;
import za.ac.cput.jobassistantapi.service.AccountService;
import za.ac.cput.jobassistantapi.service.AuthService;
import za.ac.cput.jobassistantapi.service.GoogleAuthService;
import za.ac.cput.jobassistantapi.service.PasswordResetService;

import java.time.Duration;
import java.util.Map;
@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final int MAX_REGISTRATIONS_PER_HOUR = 5;
    private static final int MAX_PASSWORD_RESET_REQUESTS_PER_HOUR = 5;

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final GoogleAuthService googleAuthService;
    private final AccountService accountService;
    private final UserRepository userRepository;
    private final RateLimiterService rateLimiterService;

    public AuthController(AuthService authService,
                           PasswordResetService passwordResetService,
                           GoogleAuthService googleAuthService,
                           AccountService accountService,
                           UserRepository userRepository,
                           RateLimiterService rateLimiterService) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
        this.googleAuthService = googleAuthService;
        this.accountService = accountService;
        this.userRepository = userRepository;
        this.rateLimiterService = rateLimiterService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
                                                  HttpServletRequest httpRequest) {
        String key = "register:" + clientIp(httpRequest);
        rateLimiterService.checkAllowed(key, MAX_REGISTRATIONS_PER_HOUR, Duration.ofHours(1));
        AuthResponse response = authService.register(request);
        rateLimiterService.recordAttempt(key, Duration.ofHours(1));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        return ResponseEntity.ok(googleAuthService.loginWithGoogle(request.getIdToken()));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request,
                                                                HttpServletRequest httpRequest) {
        String key = "forgot-password:" + clientIp(httpRequest);
        rateLimiterService.checkAllowed(key, MAX_PASSWORD_RESET_REQUESTS_PER_HOUR, Duration.ofHours(1));
        passwordResetService.forgotPassword(request);
        rateLimiterService.recordAttempt(key, Duration.ofHours(1));
        return ResponseEntity.ok(Map.of("message", "If that email is registered, a reset link has been sent."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
        return ResponseEntity.ok(Map.of("message", "Password reset successful."));
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return ResponseEntity.ok(Map.of(
                "email", user.getEmail(),
                "fullName", user.getFullName() == null ? "" : user.getFullName(),
                "createdAt", user.getCreatedAt()
        ));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(Authentication authentication,
                                                                @Valid @RequestBody ChangePasswordRequest request) {
        accountService.changePassword(authentication.getName(), request);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully."));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteAccount(Authentication authentication) {
        accountService.deleteAccount(authentication.getName());
        return ResponseEntity.noContent().build();
    }

    /** Azure App Service (and most reverse proxies) put the real client IP in X-Forwarded-For, not getRemoteAddr(). */
    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}