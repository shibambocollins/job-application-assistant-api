package za.ac.cput.jobassistantapi.service.impl;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.cput.jobassistantapi.dto.request.ChangePasswordRequest;
import za.ac.cput.jobassistantapi.exception.InvalidCredentialsException;
import za.ac.cput.jobassistantapi.exception.ResourceNotFoundException;
import za.ac.cput.jobassistantapi.model.CV;
import za.ac.cput.jobassistantapi.model.JobApplication;
import za.ac.cput.jobassistantapi.model.User;
import za.ac.cput.jobassistantapi.repository.AnalysisRepository;
import za.ac.cput.jobassistantapi.repository.CVRepository;
import za.ac.cput.jobassistantapi.repository.ChatMessageRepository;
import za.ac.cput.jobassistantapi.repository.JobApplicationRepository;
import za.ac.cput.jobassistantapi.repository.PasswordResetTokenRepository;
import za.ac.cput.jobassistantapi.repository.UserRepository;
import za.ac.cput.jobassistantapi.service.AccountService;
import za.ac.cput.jobassistantapi.service.BlobStorageService;

@Service
public class AccountServiceImpl implements AccountService {

    private final UserRepository userRepository;
    private final CVRepository cvRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final AnalysisRepository analysisRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final BlobStorageService blobStorageService;
    private final PasswordEncoder passwordEncoder;

    public AccountServiceImpl(UserRepository userRepository,
                               CVRepository cvRepository,
                               JobApplicationRepository jobApplicationRepository,
                               ChatMessageRepository chatMessageRepository,
                               AnalysisRepository analysisRepository,
                               PasswordResetTokenRepository passwordResetTokenRepository,
                               BlobStorageService blobStorageService,
                               PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.cvRepository = cvRepository;
        this.jobApplicationRepository = jobApplicationRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.analysisRepository = analysisRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.blobStorageService = blobStorageService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }

        User updated = new User.Builder()
                .copy(user)
                .setPasswordHash(passwordEncoder.encode(request.getNewPassword()))
                .build();
        userRepository.save(updated);
    }

    @Override
    @Transactional
    public void deleteAccount(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        cvRepository.findByUserId(user.getId()).ifPresent(cv -> {
            analysisRepository.findByCv_Id(cv.getId()).forEach(analysisRepository::delete);
            blobStorageService.deleteByUrl(cv.getBlobUrl());
            cvRepository.delete(cv);
        });

        for (JobApplication application : jobApplicationRepository.findByUserId(user.getId())) {
            analysisRepository.findByJobApplication_Id(application.getId()).forEach(analysisRepository::delete);
            jobApplicationRepository.delete(application);
        }

        chatMessageRepository.deleteAll(chatMessageRepository.findByUser_Id(user.getId()));
        passwordResetTokenRepository.deleteAll(passwordResetTokenRepository.findByUserId(user.getId()));

        userRepository.delete(user);
    }
}
