package za.ac.cput.jobassistantapi.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.jobassistantapi.dto.request.JobCreateRequest;
import za.ac.cput.jobassistantapi.dto.response.JobApplicationResponse;
import za.ac.cput.jobassistantapi.exception.ForbiddenException;
import za.ac.cput.jobassistantapi.exception.ResourceNotFoundException;
import za.ac.cput.jobassistantapi.model.Job;
import za.ac.cput.jobassistantapi.model.JobApplication;
import za.ac.cput.jobassistantapi.model.User;
import za.ac.cput.jobassistantapi.model.enums.ApplicationStatus;
import za.ac.cput.jobassistantapi.repository.JobApplicationRepository;
import za.ac.cput.jobassistantapi.repository.JobRepository;
import za.ac.cput.jobassistantapi.repository.UserRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobServiceImplTest {

    @Mock private JobRepository jobRepository;
    @Mock private JobApplicationRepository jobApplicationRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private JobServiceImpl jobService;

    private static final String OWNER_EMAIL = "owner@example.com";
    private static final String OTHER_EMAIL = "other@example.com";

    private User owner() {
        return new User.Builder().setId(1L).setEmail(OWNER_EMAIL).build();
    }

    private Job job() {
        return new Job.Builder().setId(10L).setTitle("Junior Dev").setCompany("Acme").setLocation("Remote").build();
    }

    private JobApplication application() {
        return new JobApplication.Builder()
                .setId(100L)
                .setUser(owner())
                .setJob(job())
                .setStatus(ApplicationStatus.SAVED)
                .setAppliedDate(LocalDate.now())
                .build();
    }

    @Test
    void addManualJob_success() {
        JobCreateRequest request = new JobCreateRequest();
        request.setTitle("Junior Dev");
        request.setCompany("Acme");
        request.setDescription("Java, Spring Boot");
        request.setLocation("Remote");

        when(userRepository.findByEmail(OWNER_EMAIL)).thenReturn(Optional.of(owner()));
        when(jobRepository.save(any(Job.class))).thenReturn(job());
        when(jobApplicationRepository.save(any(JobApplication.class))).thenReturn(application());

        JobApplicationResponse response = jobService.addManualJob(request, OWNER_EMAIL);

        assertEquals("Junior Dev", response.getJobTitle());
        assertEquals("Acme", response.getCompany());
        assertEquals(ApplicationStatus.SAVED, response.getStatus());
    }

    @Test
    void addManualJob_userNotFound_throwsResourceNotFoundException() {
        JobCreateRequest request = new JobCreateRequest();
        request.setTitle("Junior Dev");
        request.setCompany("Acme");
        request.setDescription("Java");

        when(userRepository.findByEmail(OWNER_EMAIL)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> jobService.addManualJob(request, OWNER_EMAIL));
    }

    @Test
    void getMyApplications_success() {
        when(userRepository.findByEmail(OWNER_EMAIL)).thenReturn(Optional.of(owner()));
        when(jobApplicationRepository.findByUserId(1L)).thenReturn(List.of(application()));

        List<JobApplicationResponse> result = jobService.getMyApplications(OWNER_EMAIL);

        assertEquals(1, result.size());
        assertEquals("Junior Dev", result.get(0).getJobTitle());
    }

    @Test
    void updateStatus_success() {
        JobApplication app = application();

        when(jobApplicationRepository.findById(100L)).thenReturn(Optional.of(app));
        when(jobApplicationRepository.save(any(JobApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        JobApplicationResponse response = jobService.updateStatus(100L, ApplicationStatus.APPLIED, OWNER_EMAIL);

        assertEquals(ApplicationStatus.APPLIED, response.getStatus());
    }

    @Test
    void updateStatus_notOwner_throwsForbiddenException() {
        when(jobApplicationRepository.findById(100L)).thenReturn(Optional.of(application()));

        assertThrows(ForbiddenException.class,
                () -> jobService.updateStatus(100L, ApplicationStatus.APPLIED, OTHER_EMAIL));
        verify(jobApplicationRepository, never()).save(any());
    }

    @Test
    void updateStatus_applicationNotFound_throwsResourceNotFoundException() {
        when(jobApplicationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> jobService.updateStatus(999L, ApplicationStatus.APPLIED, OWNER_EMAIL));
    }

    @Test
    void deleteApplication_success() {
        JobApplication app = application();
        when(jobApplicationRepository.findById(100L)).thenReturn(Optional.of(app));

        jobService.deleteApplication(100L, OWNER_EMAIL);

        verify(jobApplicationRepository).delete(app);
    }

    @Test
    void deleteApplication_notOwner_throwsForbiddenException() {
        when(jobApplicationRepository.findById(100L)).thenReturn(Optional.of(application()));

        assertThrows(ForbiddenException.class, () -> jobService.deleteApplication(100L, OTHER_EMAIL));
        verify(jobApplicationRepository, never()).delete(any());
    }

    @Test
    void deleteApplication_applicationNotFound_throwsResourceNotFoundException() {
        when(jobApplicationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> jobService.deleteApplication(999L, OWNER_EMAIL));
    }
}
