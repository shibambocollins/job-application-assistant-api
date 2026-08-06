package za.ac.cput.jobassistantapi.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.jobassistantapi.dto.request.ForgotPasswordRequest;
import za.ac.cput.jobassistantapi.dto.request.GoogleLoginRequest;
import za.ac.cput.jobassistantapi.dto.request.LoginRequest;
import za.ac.cput.jobassistantapi.dto.request.RegisterRequest;
import za.ac.cput.jobassistantapi.dto.request.ResetPasswordRequest;
import za.ac.cput.jobassistantapi.dto.response.AuthResponse;
import za.ac.cput.jobassistantapi.exception.ResourceNotFoundException;
import za.ac.cput.jobassistantapi.model.User;
import za.ac.cput.jobassistantapi.repository.UserRepository;
import za.ac.cput.jobassistantapi.service.AuthService;
import za.ac.cput.jobassistantapi.service.GoogleAuthService;
import za.ac.cput.jobassistantapi.service.PasswordResetService;

import java.util.Map;
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final GoogleAuthService googleAuthService;
    private final UserRepository userRepository;

    public AuthController(AuthService authService,
                           PasswordResetService passwordResetService,
                           GoogleAuthService googleAuthService,
                           UserRepository userRepository) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
        this.googleAuthService = googleAuthService;
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
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
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.forgotPassword(request);
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
}