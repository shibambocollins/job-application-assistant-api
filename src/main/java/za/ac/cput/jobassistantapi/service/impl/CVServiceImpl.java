package za.ac.cput.jobassistantapi.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import za.ac.cput.jobassistantapi.dto.response.CVDataResult;
import za.ac.cput.jobassistantapi.dto.response.CVResponse;
import za.ac.cput.jobassistantapi.model.CV;
import za.ac.cput.jobassistantapi.model.User;
import za.ac.cput.jobassistantapi.repository.CVRepository;
import za.ac.cput.jobassistantapi.repository.UserRepository;
import za.ac.cput.jobassistantapi.service.AIService;
import za.ac.cput.jobassistantapi.service.CVService;
import za.ac.cput.jobassistantapi.service.PdfExtractionService;
import za.ac.cput.jobassistantapi.service.TextCleaningService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class CVServiceImpl implements CVService {

    private final UserRepository userRepository;
    private final CVRepository cvRepository;
    private final PdfExtractionService pdfExtractionService;
    private final TextCleaningService textCleaningService;
    private final AIService aiService;

    public CVServiceImpl(CVRepository cvRepository,
                         UserRepository userRepository,
                         PdfExtractionService pdfExtractionService,
                         TextCleaningService textCleaningService,
                         AIService aiService) {
        this.cvRepository = cvRepository;
        this.userRepository = userRepository;
        this.pdfExtractionService = pdfExtractionService;
        this.textCleaningService = textCleaningService;
        this.aiService = aiService;
    }

    @Override
    public CVResponse uploadCV(MultipartFile file, String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (cvRepository.findByUserId(user.getId()).isPresent()) {
            throw new RuntimeException("User already has a CV");
        }

        try {
            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();

            Path uploadPath = Paths.get("uploads");
            Files.createDirectories(uploadPath);

            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath);

            String rawText = pdfExtractionService.extractText(file);
            String extractedText = textCleaningService.clean(rawText);

            CVDataResult cvData = aiService.extractCVData(extractedText);

            ObjectMapper mapper = new ObjectMapper();
            String skillsJson;
            try {
                skillsJson = mapper.writeValueAsString(cvData);
            } catch (Exception e) {
                skillsJson = "{}";
            }

            CV cv = new CV.Builder()
                    .setUserId(user.getId())
                    .setBlobUrl(filePath.toString())
                    .setOriginalFilename(file.getOriginalFilename())
                    .setExtractedText(extractedText)
                    .setSkillsJson(skillsJson)
                    .build();

            CV saved = cvRepository.save(cv);

            return new CVResponse(saved.getId(), "CV uploaded successfully");

        } catch (Exception e) {
            throw new RuntimeException("CV upload failed: " + e.getMessage());
        }
    }

    @Override
    public CV getCVByUserEmail(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return cvRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("CV not found"));
    }
}