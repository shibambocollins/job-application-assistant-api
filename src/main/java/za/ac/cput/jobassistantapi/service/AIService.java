package za.ac.cput.jobassistantapi.service;

import za.ac.cput.jobassistantapi.dto.response.CVDataResult;

public interface AIService {
    CVDataResult extractCVData(String cvText);
}