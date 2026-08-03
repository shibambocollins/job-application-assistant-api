package za.ac.cput.jobassistantapi.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.jobassistantapi.dto.response.AnalysisResponse;
import za.ac.cput.jobassistantapi.dto.response.JobFitResult;
import za.ac.cput.jobassistantapi.exception.ForbiddenException;
import za.ac.cput.jobassistantapi.exception.ResourceNotFoundException;
import za.ac.cput.jobassistantapi.model.Analysis;
import za.ac.cput.jobassistantapi.model.CV;
import za.ac.cput.jobassistantapi.model.Job;
import za.ac.cput.jobassistantapi.model.JobApplication;
import za.ac.cput.jobassistantapi.model.User;
import za.ac.cput.jobassistantapi.model.enums.ApplicationStatus;
import za.ac.cput.jobassistantapi.repository.AnalysisRepository;
import za.ac.cput.jobassistantapi.repository.CVRepository;
import za.ac.cput.jobassistantapi.repository.JobApplicationRepository;
import za.ac.cput.jobassistantapi.service.AIService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalysisServiceImplTest {

    @Mock private AnalysisRepository analysisRepository;
    @Mock private JobApplicationRepository jobApplicationRepository;
    @Mock private CVRepository cvRepository;
    @Mock private AIService aiService;

    @InjectMocks
    private AnalysisServiceImpl analysisService;

    private static final String OWNER_EMAIL = "owner@example.com";
    private static final String OTHER_EMAIL = "other@example.com";

    private User owner() {
        return new User.Builder().setId(1L).setEmail(OWNER_EMAIL).build();
    }

    private Job job() {
        return new Job.Builder().setId(5L).setTitle("Junior Dev").setCompany("Acme")
                .setDescription("Java, Spring Boot required").build();
    }

    private JobApplication application() {
        return new JobApplication.Builder()
                .setId(100L).setUser(owner()).setJob(job())
                .setStatus(ApplicationStatus.SAVED).setAppliedDate(LocalDate.now()).build();
    }

    private CV cv() {
        return new CV.Builder().setId(1L).setUserId(1L).setExtractedText("Java Spring Boot developer").build();
    }

    @Test
    void analyzeJobApplication_success() {
        JobApplication app = application();

        when(jobApplicationRepository.findById(100L)).thenReturn(Optional.of(app));
        when(cvRepository.findByUserId(1L)).thenReturn(Optional.of(cv()));
        when(aiService.analyzeJobFit("Java Spring Boot developer", "Java, Spring Boot required"))
                .thenReturn(new JobFitResult(75, List.of("Docker"), List.of("Java"), List.of("Learn Docker")));

        when(analysisRepository.save(any(Analysis.class))).thenAnswer(invocation -> {
            Analysis a = invocation.getArgument(0);
            return new Analysis.Builder().copy(a).setId(200L).build();
        });

        AnalysisResponse response = analysisService.analyzeJobApplication(100L, OWNER_EMAIL);

        assertEquals(200L, response.getId());
        assertEquals(75, response.getMatchScore());
        assertEquals(List.of("Docker"), response.getMissingSkills());
        assertEquals("Junior Dev", response.getJobTitle());
    }

    @Test
    void analyzeJobApplication_calledTwice_createsTwoSeparateRecordsNotOverwrite() {
        JobApplication app = application();

        when(jobApplicationRepository.findById(100L)).thenReturn(Optional.of(app));
        when(cvRepository.findByUserId(1L)).thenReturn(Optional.of(cv()));
        when(aiService.analyzeJobFit(anyString(), anyString()))
                .thenReturn(new JobFitResult(60, List.of("Docker"), List.of("Java"), List.of("Learn Docker")))
                .thenReturn(new JobFitResult(70, List.of(), List.of("Java", "Docker"), List.of()));

        ArgumentCaptor<Analysis> captor = ArgumentCaptor.forClass(Analysis.class);
        when(analysisRepository.save(captor.capture())).thenAnswer(invocation -> {
            Analysis a = invocation.getArgument(0);
            long generatedId = a.getMatchScore() == 60 ? 200L : 201L;
            return new Analysis.Builder().copy(a).setId(generatedId).build();
        });

        AnalysisResponse first = analysisService.analyzeJobApplication(100L, OWNER_EMAIL);
        AnalysisResponse second = analysisService.analyzeJobApplication(100L, OWNER_EMAIL);

        assertEquals(200L, first.getId());
        assertEquals(201L, second.getId());
        assertEquals(2, captor.getAllValues().size());
        captor.getAllValues().forEach(a -> assertNull(a.getId()));
    }

    @Test
    void analyzeJobApplication_applicationNotFound_throwsResourceNotFoundException() {
        when(jobApplicationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> analysisService.analyzeJobApplication(999L, OWNER_EMAIL));
    }

    @Test
    void analyzeJobApplication_notOwner_throwsForbiddenException() {
        when(jobApplicationRepository.findById(100L)).thenReturn(Optional.of(application()));

        assertThrows(ForbiddenException.class,
                () -> analysisService.analyzeJobApplication(100L, OTHER_EMAIL));
    }

    @Test
    void analyzeJobApplication_noCv_throwsResourceNotFoundException() {
        when(jobApplicationRepository.findById(100L)).thenReturn(Optional.of(application()));
        when(cvRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> analysisService.analyzeJobApplication(100L, OWNER_EMAIL));
    }

    @Test
    void getLatestAnalysis_success() {
        JobApplication app = application();
        Analysis analysis = new Analysis.Builder()
                .setId(200L).setJobApplication(app).setCv(cv())
                .setMatchScore(80).setMissingSkills("[]").setStrengths("[]").setAiSuggestions("[]")
                .build();

        when(jobApplicationRepository.findById(100L)).thenReturn(Optional.of(app));
        when(analysisRepository.findTopByJobApplication_IdOrderByCreatedAtDesc(100L))
                .thenReturn(Optional.of(analysis));

        AnalysisResponse response = analysisService.getLatestAnalysis(100L, OWNER_EMAIL);

        assertEquals(80, response.getMatchScore());
        assertTrue(response.getMissingSkills().isEmpty());
    }

    @Test
    void getLatestAnalysis_notOwner_throwsForbiddenException() {
        when(jobApplicationRepository.findById(100L)).thenReturn(Optional.of(application()));

        assertThrows(ForbiddenException.class,
                () -> analysisService.getLatestAnalysis(100L, OTHER_EMAIL));
    }

    @Test
    void getLatestAnalysis_noAnalysisYet_throwsResourceNotFoundException() {
        when(jobApplicationRepository.findById(100L)).thenReturn(Optional.of(application()));
        when(analysisRepository.findTopByJobApplication_IdOrderByCreatedAtDesc(100L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> analysisService.getLatestAnalysis(100L, OWNER_EMAIL));
    }
}
