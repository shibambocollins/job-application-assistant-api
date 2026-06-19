package za.ac.cput.jobassistantapi.service.impl;

import org.springframework.stereotype.Service;
import za.ac.cput.jobassistantapi.dto.request.CVUploadRequest;
import za.ac.cput.jobassistantapi.dto.response.CVResponse;
import za.ac.cput.jobassistantapi.dto.response.CVUploadResponse;
import za.ac.cput.jobassistantapi.model.CV;
import za.ac.cput.jobassistantapi.model.User;
import za.ac.cput.jobassistantapi.repository.CVRepository;
import za.ac.cput.jobassistantapi.repository.UserRepository;
import za.ac.cput.jobassistantapi.service.CVService;
import org.springframework.web.multipart.MultipartFile;
import za.ac.cput.jobassistantapi.service.PdfExtractionService;
import za.ac.cput.jobassistantapi.service.SkillExtractionService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

@Service
public class CVServiceImpl implements CVService {

    private final UserRepository userRepository;
    private final CVRepository cvRepository;
    private final PdfExtractionService pdfExtractionService;
    private final SkillExtractionService skillExtractionService;

    public CVServiceImpl(CVRepository cvRepository,
                         UserRepository userRepository, PdfExtractionService pdfExtractionService, SkillExtractionService skillExtractionService) {
        this.cvRepository = cvRepository;
        this.userRepository = userRepository;
        this.pdfExtractionService = pdfExtractionService;
        this.skillExtractionService = skillExtractionService;
    }

    @Override
    public CVResponse uploadCV(CVUploadRequest request, String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (cvRepository.findByUserId(user.getId()).isPresent()) {
            throw new RuntimeException("User already has a CV");
        }

        try {

            String fileName =
                    UUID.randomUUID() + "_" + request.getFile().getOriginalFilename();

            Path uploadPath = Paths.get("uploads");
            Files.createDirectories(uploadPath);

            Path filePath = uploadPath.resolve(fileName);
            Files.copy(request.getFile().getInputStream(), filePath);


            String extractedText = pdfExtractionService.extractText(request.getFile());

            Set<String> skills = skillExtractionService.extractSkills(extractedText);


            CV cv = new CV.Builder()
                    .setUserId(user.getId())
                    .setBlobUrl(filePath.toString())
                    .setOriginalFilename(request.getFile().getOriginalFilename())
                    .setExtractedText(extractedText)
                    .setSkillsJson(skills.toString())
                    .build();

            CV saved = cvRepository.save(cv);

            return new CVResponse(saved.getId(), "CV uploaded successfully");

        } catch (Exception e) {
            throw new RuntimeException("CV upload failed: " + e.getMessage());
        }
    }
  /*  @Override
    public CVUploadResponse uploadFile(MultipartFile file, String email) {

        try {

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String fileName =
                    UUID.randomUUID() + "_" + file.getOriginalFilename();

            Path uploadPath = Paths.get("uploads");

            Files.createDirectories(uploadPath);

            Path filePath = uploadPath.resolve(fileName);

            Files.copy(file.getInputStream(), filePath);

            CV cv = new CV.Builder()
                    .setUserId(user.getId())
                    .setBlobUrl(filePath.toString())
                    .setOriginalFilename(file.getOriginalFilename())
                    .build();

            CV saved = cvRepository.save(cv);

            return new CVUploadResponse(
                    saved.getId(),
                    saved.getOriginalFilename()
            );

        } catch (Exception e) {
            throw new RuntimeException("File upload failed"+ e.getMessage());
        }
    }*/

    @Override
    public CV getCVByUserEmail(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return cvRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("CV not found"));
    }
}