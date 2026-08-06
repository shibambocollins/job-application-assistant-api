package za.ac.cput.jobassistantapi.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import za.ac.cput.jobassistantapi.dto.request.ChangePasswordRequest;
import za.ac.cput.jobassistantapi.exception.InvalidCredentialsException;
import za.ac.cput.jobassistantapi.exception.ResourceNotFoundException;
import za.ac.cput.jobassistantapi.model.Analysis;
import za.ac.cput.jobassistantapi.model.CV;
import za.ac.cput.jobassistantapi.model.ChatMessage;
import za.ac.cput.jobassistantapi.model.JobApplication;
import za.ac.cput.jobassistantapi.model.PasswordResetToken;
import za.ac.cput.jobassistantapi.model.User;
import za.ac.cput.jobassistantapi.repository.AnalysisRepository;
import za.ac.cput.jobassistantapi.repository.CVRepository;
import za.ac.cput.jobassistantapi.repository.ChatMessageRepository;
import za.ac.cput.jobassistantapi.repository.JobApplicationRepository;
import za.ac.cput.jobassistantapi.repository.PasswordResetTokenRepository;
import za.ac.cput.jobassistantapi.repository.UserRepository;
import za.ac.cput.jobassistantapi.service.BlobStorageService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private CVRepository cvRepository;
    @Mock private JobApplicationRepository jobApplicationRepository;
    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private AnalysisRepository analysisRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private BlobStorageService blobStorageService;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AccountServiceImpl accountService;

    private static final String EMAIL = "user@example.com";

    private User user() {
        return new User.Builder().setId(1L).setEmail(EMAIL).setPasswordHash("hashed").build();
    }

    @Test
    void changePassword_correctCurrentPassword_updatesHash() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldpass123");
        request.setNewPassword("newpass456");

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user()));
        when(passwordEncoder.matches("oldpass123", "hashed")).thenReturn(true);
        when(passwordEncoder.encode("newpass456")).thenReturn("new-hashed");

        accountService.changePassword(EMAIL, request);

        verify(userRepository).save(any(User.class));
    }

    @Test
    void changePassword_wrongCurrentPassword_throwsInvalidCredentials() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("wrongpass");
        request.setNewPassword("newpass456");

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user()));
        when(passwordEncoder.matches("wrongpass", "hashed")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> accountService.changePassword(EMAIL, request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_userNotFound_throwsResourceNotFoundException() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("x");
        request.setNewPassword("newpass456");

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> accountService.changePassword(EMAIL, request));
    }

    @Test
    void deleteAccount_withCvAndJobsAndChat_deletesEverythingInOrder() {
        User user = user();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        CV cv = new CV.Builder().setId(5L).setUserId(1L).setBlobUrl("https://blob/cv.pdf").build();
        when(cvRepository.findByUserId(1L)).thenReturn(Optional.of(cv));
        when(analysisRepository.findByCv_Id(5L)).thenReturn(List.of(new Analysis.Builder().setId(100L).build()));

        JobApplication app = new JobApplication.Builder().setId(7L).setUser(user).build();
        when(jobApplicationRepository.findByUserId(1L)).thenReturn(List.of(app));
        when(analysisRepository.findByJobApplication_Id(7L)).thenReturn(List.of(new Analysis.Builder().setId(101L).build()));

        when(chatMessageRepository.findByUser_Id(1L)).thenReturn(List.of(new ChatMessage.Builder().setId(200L).build()));
        when(passwordResetTokenRepository.findByUserId(1L)).thenReturn(List.of(new PasswordResetToken.Builder().setId(300L).build()));

        accountService.deleteAccount(EMAIL);

        verify(blobStorageService).deleteByUrl("https://blob/cv.pdf");
        verify(analysisRepository, times(2)).delete(any(Analysis.class));
        verify(cvRepository).delete(cv);
        verify(jobApplicationRepository).delete(app);
        verify(chatMessageRepository).deleteAll(any());
        verify(passwordResetTokenRepository).deleteAll(any());
        verify(userRepository).delete(user);
    }

    @Test
    void deleteAccount_noCv_skipsBlobDeletion() {
        User user = user();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(cvRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(jobApplicationRepository.findByUserId(1L)).thenReturn(List.of());
        when(chatMessageRepository.findByUser_Id(1L)).thenReturn(List.of());
        when(passwordResetTokenRepository.findByUserId(1L)).thenReturn(List.of());

        accountService.deleteAccount(EMAIL);

        verify(blobStorageService, never()).deleteByUrl(any());
        verify(cvRepository, never()).delete(any());
        verify(userRepository).delete(user);
    }

    @Test
    void deleteAccount_userNotFound_throwsResourceNotFoundException() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> accountService.deleteAccount(EMAIL));
        verify(userRepository, never()).delete(any(User.class));
    }
}
