package za.ac.cput.jobassistantapi.service;

import za.ac.cput.jobassistantapi.dto.response.CVDataResult;
import za.ac.cput.jobassistantapi.dto.response.JobFitResult;

public interface AIService {
    CVDataResult extractCVData(String cvText);

    JobFitResult analyzeJobFit(String cvText, String jobDescription);
}