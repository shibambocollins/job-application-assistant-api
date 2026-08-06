package za.ac.cput.jobassistantapi.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import za.ac.cput.jobassistantapi.dto.request.ForgotPasswordRequest;
import za.ac.cput.jobassistantapi.dto.request.ResetPasswordRequest;
import za.ac.cput.jobassistantapi.exception.InvalidRequestException;
import za.ac.cput.jobassistantapi.model.PasswordResetToken;
import za.ac.cput.jobassistantapi.model.User;
import za.ac.cput.jobassistantapi.repository.PasswordResetTokenRepository;
import za.ac.cput.jobassistantapi.repository.UserRepository;
import za.ac.cput.jobassistantapi.service.EmailService;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private PasswordResetTokenRepository tokenRepository;
    @Mock private EmailService emailService;

    @InjectMocks
    private PasswordResetServiceImpl passwordResetService;

    @Test
    void forgotPassword_unknownEmail_doesNothingSilently() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("nobody@example.com");
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        passwordResetService.forgotPassword(request);

        verify(tokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(any(), any());
    }

    @Test
    void forgotPassword_knownEmail_savesTokenAndSendsEmail() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("user@example.com");
        User user = new User.Builder().setId(1L).setEmail("user@example.com").build();
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        passwordResetService.forgotPassword(request);

        verify(tokenRepository).save(any(PasswordResetToken.class));
        verify(emailService).sendPasswordResetEmail(eq("user@example.com"), anyString());
    }

    @Test
    void resetPassword_validToken_updatesPasswordAndMarksTokenUsed() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("valid-token");
        request.setNewPassword("newpassword123");

        PasswordResetToken token = new PasswordResetToken.Builder()
                .setId(1L).setUserId(1L).setToken("valid-token")
                .setExpiresAt(LocalDateTime.now().plusMinutes(10)).setUsed(false).build();
        when(tokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));

        User user = new User.Builder().setId(1L).setEmail("user@example.com").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newpassword123")).thenReturn("new-hash");

        passwordResetService.resetPassword(request);

        verify(userRepository).save(any(User.class));
        verify(tokenRepository).save(any(PasswordResetToken.class));
    }

    @Test
    void resetPassword_expiredToken_throwsInvalidRequestException() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("expired-token");
        request.setNewPassword("newpassword123");

        PasswordResetToken token = new PasswordResetToken.Builder()
                .setId(1L).setUserId(1L).setToken("expired-token")
                .setExpiresAt(LocalDateTime.now().minusMinutes(1)).setUsed(false).build();
        when(tokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

        assertThrows(InvalidRequestException.class, () -> passwordResetService.resetPassword(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_alreadyUsedToken_throwsInvalidRequestException() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("used-token");
        request.setNewPassword("newpassword123");

        PasswordResetToken token = new PasswordResetToken.Builder()
                .setId(1L).setUserId(1L).setToken("used-token")
                .setExpiresAt(LocalDateTime.now().plusMinutes(10)).setUsed(true).build();
        when(tokenRepository.findByToken("used-token")).thenReturn(Optional.of(token));

        assertThrows(InvalidRequestException.class, () -> passwordResetService.resetPassword(request));
    }

    @Test
    void resetPassword_unknownToken_throwsInvalidRequestException() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("nonexistent");
        request.setNewPassword("newpassword123");

        when(tokenRepository.findByToken("nonexistent")).thenReturn(Optional.empty());

        assertThrows(InvalidRequestException.class, () -> passwordResetService.resetPassword(request));
    }
}
