package za.ac.cput.jobassistantapi.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import za.ac.cput.jobassistantapi.dto.response.CVDataResult;
import za.ac.cput.jobassistantapi.dto.response.CVResponse;
import za.ac.cput.jobassistantapi.exception.DuplicateResourceException;
import za.ac.cput.jobassistantapi.exception.InvalidRequestException;
import za.ac.cput.jobassistantapi.exception.ResourceNotFoundException;
import za.ac.cput.jobassistantapi.model.CV;
import za.ac.cput.jobassistantapi.model.User;
import za.ac.cput.jobassistantapi.repository.CVRepository;
import za.ac.cput.jobassistantapi.repository.UserRepository;
import za.ac.cput.jobassistantapi.service.AIService;
import za.ac.cput.jobassistantapi.service.BlobStorageService;
import za.ac.cput.jobassistantapi.service.CVService;
import za.ac.cput.jobassistantapi.service.PdfExtractionService;
import za.ac.cput.jobassistantapi.service.TextCleaningService;
import za.ac.cput.jobassistantapi.service.WordExtractionService;

import java.util.UUID;

@Service
public class CVServiceImpl implements CVService {

    private final UserRepository userRepository;
    private final CVRepository cvRepository;
    private final PdfExtractionService pdfExtractionService;
    private final WordExtractionService wordExtractionService;
    private final TextCleaningService textCleaningService;
    private final AIService aiService;
    private final BlobStorageService blobStorageService;

    public CVServiceImpl(CVRepository cvRepository,
                         UserRepository userRepository,
                         PdfExtractionService pdfExtractionService,
                         WordExtractionService wordExtractionService,
                         TextCleaningService textCleaningService,
                         AIService aiService,
                         BlobStorageService blobStorageService) {
        this.cvRepository = cvRepository;
        this.userRepository = userRepository;
        this.pdfExtractionService = pdfExtractionService;
        this.wordExtractionService = wordExtractionService;
        this.textCleaningService = textCleaningService;
        this.aiService = aiService;
        this.blobStorageService = blobStorageService;
    }

    @Override
    public CVResponse uploadCV(MultipartFile file, String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (cvRepository.findByUserId(user.getId()).isPresent()) {
            throw new DuplicateResourceException("User already has a CV. Use the replace endpoint to re-upload.");
        }

        ProcessedFile processed = processFile(file);

        CV cv = new CV.Builder()
                .setUserId(user.getId())
                .setBlobUrl(processed.blobUrl)
                .setOriginalFilename(file.getOriginalFilename())
                .setExtractedText(processed.extractedText)
                .setSkillsJson(processed.skillsJson)
                .build();

        CV saved = cvRepository.save(cv);

        return new CVResponse(saved.getId(), "CV uploaded successfully");
    }

    @Override
    public CVResponse replaceCV(MultipartFile file, String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        CV existing = cvRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No existing CV to replace — upload one first"));

        ProcessedFile processed = processFile(file);

        blobStorageService.deleteByUrl(existing.getBlobUrl());

        CV updated = new CV.Builder()
                .copy(existing)
                .setBlobUrl(processed.blobUrl)
                .setOriginalFilename(file.getOriginalFilename())
                .setExtractedText(processed.extractedText)
                .setSkillsJson(processed.skillsJson)
                .build();

        CV saved = cvRepository.save(updated);

        return new CVResponse(saved.getId(), "CV replaced successfully");
    }

    @Override
    public CV getCVByUserEmail(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return cvRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("CV not found"));
    }

    private ProcessedFile processFile(MultipartFile file) {

        if (file.isEmpty()) {
            throw new InvalidRequestException("Uploaded file is empty");
        }

        String extension = getExtension(file.getOriginalFilename());
        String contentType = file.getContentType();

        boolean isPdf = "pdf".equals(extension) || "application/pdf".equals(contentType);

        boolean isWord = "docx".equals(extension) || "doc".equals(extension)
                || "application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(contentType)
                || "application/msword".equals(contentType);

        boolean isImage = "jpg".equals(extension) || "jpeg".equals(extension) || "png".equals(extension)
                || (contentType != null && contentType.startsWith("image/"));

        if (!isPdf && !isWord && !isImage) {
            throw new InvalidRequestException("Unsupported file type. Supported: PDF, DOC, DOCX, JPG, PNG");
        }

        try {
            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();

            String blobUrl = blobStorageService.upload(file, fileName);

            String rawText;
            if (isPdf) {
                rawText = pdfExtractionService.extractText(file);
            } else if (isWord) {
                rawText = wordExtractionService.extractText(file);
            } else {
                rawText = aiService.extractTextFromImage(file.getBytes(), contentType);
            }

            String extractedText = textCleaningService.clean(rawText);

            CVDataResult cvData = aiService.extractCVData(extractedText);

            ObjectMapper mapper = new ObjectMapper();
            String skillsJson;
            try {
                skillsJson = mapper.writeValueAsString(cvData);
            } catch (Exception e) {
                skillsJson = "{}";
            }

            return new ProcessedFile(blobUrl, extractedText, skillsJson);

        } catch (Exception e) {
            throw new RuntimeException("CV upload failed: " + e.getMessage());
        }
    }

    private String getExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1).toLowerCase() : "";
    }

    private record ProcessedFile(String blobUrl, String extractedText, String skillsJson) {
    }
}
