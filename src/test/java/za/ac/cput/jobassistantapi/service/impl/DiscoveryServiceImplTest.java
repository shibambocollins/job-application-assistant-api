package za.ac.cput.jobassistantapi.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.jobassistantapi.dto.response.JobApplicationResponse;
import za.ac.cput.jobassistantapi.exception.ResourceNotFoundException;
import za.ac.cput.jobassistantapi.model.CV;
import za.ac.cput.jobassistantapi.model.Job;
import za.ac.cput.jobassistantapi.model.JobApplication;
import za.ac.cput.jobassistantapi.model.User;
import za.ac.cput.jobassistantapi.model.enums.ApplicationStatus;
import za.ac.cput.jobassistantapi.repository.CVRepository;
import za.ac.cput.jobassistantapi.repository.JobApplicationRepository;
import za.ac.cput.jobassistantapi.repository.JobRepository;
import za.ac.cput.jobassistantapi.repository.UserRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiscoveryServiceImplTest {

    @Mock private MuseApiClient museApiClient;
    @Mock private JobRepository jobRepository;
    @Mock private JobApplicationRepository jobApplicationRepository;
    @Mock private UserRepository userRepository;
    @Mock private CVRepository cvRepository;

    @InjectMocks
    private DiscoveryServiceImpl discoveryService;

    private static final String EMAIL = "user@example.com";
    private final ObjectMapper mapper = new ObjectMapper();

    private User user() {
        return new User.Builder().setId(1L).setEmail(EMAIL).build();
    }

    private JsonNode museJob(String id, String title) throws Exception {
        return mapper.readTree("""
                {
                  "id": "%s",
                  "name": "%s",
                  "company": {"name": "Acme"},
                  "contents": "Job description",
                  "locations": [{"name": "Remote"}]
                }
                """.formatted(id, title));
    }

    @Test
    void discoverJobs_userNotFound_throwsResourceNotFoundException() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> discoveryService.discoverJobs(EMAIL));
        verify(museApiClient, never()).fetchJobs(any(), any());
    }

    @Test
    void discoverJobs_noCv_throwsResourceNotFoundException() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user()));
        when(cvRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> discoveryService.discoverJobs(EMAIL));
        verify(museApiClient, never()).fetchJobs(any(), any());
    }

    @Test
    void discoverJobs_newJob_createsJobAndApplication() throws Exception {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user()));
        when(cvRepository.findByUserId(1L)).thenReturn(Optional.of(new CV.Builder().setId(1L).build()));
        when(museApiClient.fetchJobs("Software Engineering", "Entry Level"))
                .thenReturn(List.of(museJob("ext-1", "Junior Developer")));

        when(jobRepository.findByExternalId("ext-1")).thenReturn(Optional.empty());
        Job savedJob = new Job.Builder().setId(10L).setExternalId("ext-1")
                .setTitle("Junior Developer").setCompany("Acme").setLocation("Remote").build();
        when(jobRepository.save(any(Job.class))).thenReturn(savedJob);

        when(jobApplicationRepository.existsByUserIdAndJobId(1L, 10L)).thenReturn(false);
        JobApplication savedApp = new JobApplication.Builder()
                .setId(50L).setUser(user()).setJob(savedJob)
                .setStatus(ApplicationStatus.SAVED).setAppliedDate(LocalDate.now()).build();
        when(jobApplicationRepository.save(any(JobApplication.class))).thenReturn(savedApp);

        List<JobApplicationResponse> results = discoveryService.discoverJobs(EMAIL);

        assertEquals(1, results.size());
        assertEquals("Junior Developer", results.get(0).getJobTitle());
        verify(jobRepository).save(any(Job.class));
        verify(jobApplicationRepository).save(any(JobApplication.class));
    }

    @Test
    void discoverJobs_alreadyApplied_skipsDuplicateApplication() throws Exception {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user()));
        when(cvRepository.findByUserId(1L)).thenReturn(Optional.of(new CV.Builder().setId(1L).build()));
        when(museApiClient.fetchJobs("Software Engineering", "Entry Level"))
                .thenReturn(List.of(museJob("ext-2", "Backend Dev")));

        Job existingJob = new Job.Builder().setId(20L).setExternalId("ext-2")
                .setTitle("Backend Dev").setCompany("Acme").build();
        when(jobRepository.findByExternalId("ext-2")).thenReturn(Optional.of(existingJob));
        when(jobApplicationRepository.existsByUserIdAndJobId(1L, 20L)).thenReturn(true);

        List<JobApplicationResponse> results = discoveryService.discoverJobs(EMAIL);

        assertTrue(results.isEmpty());
        verify(jobRepository, never()).save(any());
        verify(jobApplicationRepository, never()).save(any());
    }

    @Test
    void discoverJobs_existingJobNotYetApplied_createsApplicationWithoutRecreatingJob() throws Exception {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user()));
        when(cvRepository.findByUserId(1L)).thenReturn(Optional.of(new CV.Builder().setId(1L).build()));
        when(museApiClient.fetchJobs("Software Engineering", "Entry Level"))
                .thenReturn(List.of(museJob("ext-3", "Full Stack Dev")));

        Job existingJob = new Job.Builder().setId(30L).setExternalId("ext-3")
                .setTitle("Full Stack Dev").setCompany("Acme").build();
        when(jobRepository.findByExternalId("ext-3")).thenReturn(Optional.of(existingJob));
        when(jobApplicationRepository.existsByUserIdAndJobId(1L, 30L)).thenReturn(false);

        JobApplication savedApp = new JobApplication.Builder()
                .setId(60L).setUser(user()).setJob(existingJob)
                .setStatus(ApplicationStatus.SAVED).setAppliedDate(LocalDate.now()).build();
        when(jobApplicationRepository.save(any(JobApplication.class))).thenReturn(savedApp);

        List<JobApplicationResponse> results = discoveryService.discoverJobs(EMAIL);

        assertEquals(1, results.size());
        verify(jobRepository, never()).save(any());
        verify(jobApplicationRepository).save(any(JobApplication.class));
    }
}
