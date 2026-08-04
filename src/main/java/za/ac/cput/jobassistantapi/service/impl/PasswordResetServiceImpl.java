package za.ac.cput.jobassistantapi.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import za.ac.cput.jobassistantapi.dto.request.ForgotPasswordRequest;
import za.ac.cput.jobassistantapi.dto.request.ResetPasswordRequest;
import za.ac.cput.jobassistantapi.exception.InvalidRequestException;
import za.ac.cput.jobassistantapi.model.PasswordResetToken;
import za.ac.cput.jobassistantapi.model.User;
import za.ac.cput.jobassistantapi.repository.PasswordResetTokenRepository;
import za.ac.cput.jobassistantapi.repository.UserRepository;
import za.ac.cput.jobassistantapi.service.EmailService;
import za.ac.cput.jobassistantapi.service.PasswordResetService;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final int TOKEN_VALID_MINUTES = 30;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;

    @Value("${frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    public PasswordResetServiceImpl(UserRepository userRepository,
                                     PasswordEncoder passwordEncoder,
                                     PasswordResetTokenRepository tokenRepository,
                                     EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());

        // Always behave the same whether or not the email is registered, so this endpoint
        // can't be used to enumerate which addresses have accounts.
        if (userOpt.isEmpty()) {
            return;
        }

        User user = userOpt.get();
        String token = UUID.randomUUID().toString();

        PasswordResetToken resetToken = new PasswordResetToken.Builder()
                .setUserId(user.getId())
                .setToken(token)
                .setExpiresAt(LocalDateTime.now().plusMinutes(TOKEN_VALID_MINUTES))
                .setUsed(false)
                .build();
        tokenRepository.save(resetToken);

        String resetLink = frontendBaseUrl + "/reset-password?token=" + token;
        emailService.sendPasswordResetEmail(user.getEmail(), resetLink);
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = tokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new InvalidRequestException("Invalid or expired reset link"));

        if (resetToken.isUsed() || resetToken.isExpired()) {
            throw new InvalidRequestException("Invalid or expired reset link");
        }

        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new InvalidRequestException("Invalid or expired reset link"));

        User updatedUser = new User.Builder()
                .copy(user)
                .setPasswordHash(passwordEncoder.encode(request.getNewPassword()))
                .build();
        userRepository.save(updatedUser);

        PasswordResetToken usedToken = new PasswordResetToken.Builder()
                .copy(resetToken)
                .setUsed(true)
                .build();
        tokenRepository.save(usedToken);
    }
}
