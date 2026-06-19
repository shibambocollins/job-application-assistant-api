package za.ac.cput.jobassistantapi.service;

import org.springframework.web.multipart.MultipartFile;

public interface PdfExtractionService {

    String extractText(MultipartFile file);

}