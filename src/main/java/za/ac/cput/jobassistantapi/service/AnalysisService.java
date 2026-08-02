package za.ac.cput.jobassistantapi.service;

import za.ac.cput.jobassistantapi.dto.response.AnalysisResponse;

public interface AnalysisService {

    AnalysisResponse analyzeJobApplication(Long jobApplicationId, String email);

    AnalysisResponse getLatestAnalysis(Long jobApplicationId, String email);
}
