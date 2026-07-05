package za.ac.cput.jobassistantapi.service;

import za.ac.cput.jobassistantapi.dto.response.SkillExtractionResult;

public interface SkillExtractionService {
    SkillExtractionResult extract(String text);
}