package za.ac.cput.jobassistantapi.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import za.ac.cput.jobassistantapi.dto.response.AnalysisResponse;
import za.ac.cput.jobassistantapi.dto.response.JobFitResult;
import za.ac.cput.jobassistantapi.model.Analysis;
import za.ac.cput.jobassistantapi.model.CV;
import za.ac.cput.jobassistantapi.model.JobApplication;
import za.ac.cput.jobassistantapi.repository.AnalysisRepository;
import za.ac.cput.jobassistantapi.repository.CVRepository;
import za.ac.cput.jobassistantapi.repository.JobApplicationRepository;
import za.ac.cput.jobassistantapi.service.AIService;
import za.ac.cput.jobassistantapi.service.AnalysisService;

import java.util.List;

@Service
public class AnalysisServiceImpl implements AnalysisService {

    private final AnalysisRepository analysisRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final CVRepository cvRepository;
    private final AIService aiService;
    private final ObjectMapper mapper = new ObjectMapper();

    public AnalysisServiceImpl(AnalysisRepository analysisRepository,
                               JobApplicationRepository jobApplicationRepository,
                               CVRepository cvRepository,
                               AIService aiService) {
        this.analysisRepository = analysisRepository;
        this.jobApplicationRepository = jobApplicationRepository;
        this.cvRepository = cvRepository;
        this.aiService = aiService;
    }

    @Override
    public AnalysisResponse analyzeJobApplication(Long jobApplicationId, String email) {

        JobApplication application = getOwnedApplication(jobApplicationId, email);

        CV cv = cvRepository.findByUserId(application.getUser().getId())
                .orElseThrow(() -> new RuntimeException("Upload a CV first"));

        JobFitResult result = aiService.analyzeJobFit(
                cv.getExtractedText(),
                application.getJob().getDescription()
        );

        Analysis analysis = new Analysis.Builder()
                .setJobApplication(application)
                .setCv(cv)
                .setMatchScore(result.getMatchScore())
                .setMissingSkills(toJson(result.getMissingSkills()))
                .setStrengths(toJson(result.getStrengths()))
                .setAiSuggestions(toJson(result.getSuggestions()))
                .build();

        Analysis saved = analysisRepository.save(analysis);

        return toResponse(saved);
    }

    @Override
    public AnalysisResponse getLatestAnalysis(Long jobApplicationId, String email) {

        getOwnedApplication(jobApplicationId, email);

        Analysis analysis = analysisRepository
                .findTopByJobApplication_IdOrderByCreatedAtDesc(jobApplicationId)
                .orElseThrow(() -> new RuntimeException("No analysis found for this application"));

        return toResponse(analysis);
    }

    private JobApplication getOwnedApplication(Long jobApplicationId, String email) {
        JobApplication application = jobApplicationRepository.findById(jobApplicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        if (!application.getUser().getEmail().equals(email)) {
            throw new RuntimeException("Not your application");
        }

        return application;
    }

    private AnalysisResponse toResponse(Analysis analysis) {
        JobApplication application = analysis.getJobApplication();

        return new AnalysisResponse(
                analysis.getId(),
                application.getId(),
                application.getJob().getTitle(),
                application.getJob().getCompany(),
                analysis.getMatchScore(),
                fromJson(analysis.getMissingSkills()),
                fromJson(analysis.getStrengths()),
                fromJson(analysis.getAiSuggestions()),
                analysis.getCreatedAt()
        );
    }

    private String toJson(List<String> values) {
        try {
            return mapper.writeValueAsString(values);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<String> fromJson(String json) {
        try {
            return mapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
