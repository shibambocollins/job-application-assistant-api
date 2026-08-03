package za.ac.cput.jobassistantapi.service;

import org.springframework.web.multipart.MultipartFile;

public interface WordExtractionService {

    String extractText(MultipartFile file);

}
