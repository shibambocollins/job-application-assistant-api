package za.ac.cput.jobassistantapi.service;

import org.springframework.web.multipart.MultipartFile;
import za.ac.cput.jobassistantapi.dto.response.CVResponse;
import za.ac.cput.jobassistantapi.model.CV;

public interface CVService {
    CVResponse uploadCV(MultipartFile file, String email);
    CV getCVByUserEmail(String email);
}