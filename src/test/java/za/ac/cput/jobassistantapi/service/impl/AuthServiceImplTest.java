package za.ac.cput.jobassistantapi.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import za.ac.cput.jobassistantapi.dto.request.LoginRequest;
import za.ac.cput.jobassistantapi.dto.request.RegisterRequest;
import za.ac.cput.jobassistantapi.dto.response.AuthResponse;
import za.ac.cput.jobassistantapi.exception.DuplicateResourceException;
import za.ac.cput.jobassistantapi.exception.InvalidCredentialsException;
import za.ac.cput.jobassistantapi.exception.ResourceNotFoundException;
import za.ac.cput.jobassistantapi.model.User;
import za.ac.cput.jobassistantapi.repository.UserRepository;
import za.ac.cput.jobassistantapi.security.JwtService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest registerRequest(String email, String password, String fullName) {
        RegisterRequest request = new RegisterRequest();
        request.setEmail(email);
        request.setPassword(password);
        request.setFullName(fullName);
        return request;
    }

    private LoginRequest loginRequest(String email, String password) {
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }

    @Test
    void register_success_savesUserAndReturnsToken() {
        RegisterRequest request = registerRequest("new@example.com", "Password123!", "New User");

        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("hashed-password");
        when(jwtService.generateToken("new@example.com")).thenReturn("token-123");

        AuthResponse response = authService.register(request);

        assertEquals("REGISTERED_SUCCESSFULLY", response.getMessage());
        assertEquals("token-123", response.getToken());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_duplicateEmail_throwsDuplicateResourceException() {
        RegisterRequest request = registerRequest("existing@example.com", "Password123!", "Existing User");

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_success_returnsToken() {
        LoginRequest request = loginRequest("user@example.com", "Password123!");

        User user = new User.Builder()
                .setEmail("user@example.com")
                .setPasswordHash("hashed-password")
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123!", "hashed-password")).thenReturn(true);
        when(jwtService.generateToken("user@example.com")).thenReturn("token-456");

        AuthResponse response = authService.login(request);

        assertEquals("LOGIN_SUCCESSFUL", response.getMessage());
        assertEquals("token-456", response.getToken());
    }

    @Test
    void login_wrongPassword_throwsInvalidCredentialsException() {
        LoginRequest request = loginRequest("user@example.com", "WrongPassword");

        User user = new User.Builder()
                .setEmail("user@example.com")
                .setPasswordHash("hashed-password")
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword", "hashed-password")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void login_userNotFound_throwsResourceNotFoundException() {
        LoginRequest request = loginRequest("nobody@example.com", "Password123!");

        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authService.login(request));
    }
}
