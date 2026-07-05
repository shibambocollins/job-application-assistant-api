package za.ac.cput.jobassistantapi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.jobassistantapi.dto.response.CVResponse;
import za.ac.cput.jobassistantapi.model.CV;
import za.ac.cput.jobassistantapi.service.CVService;
import org.springframework.web.multipart.MultipartFile;
@RestController
@RequestMapping("/cv")
public class CVController {

    private final CVService cvService;

    public CVController(CVService cvService) {
        this.cvService = cvService;
    }

    @PostMapping("/upload")
    public ResponseEntity<CVResponse> uploadCV(
            @RequestPart("file") MultipartFile file,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                cvService.uploadCV(file, authentication.getName())
        );
    }

    @GetMapping("/my-cv")
    public ResponseEntity<CV> getMyCV(Authentication authentication) {
        return ResponseEntity.ok(
                cvService.getCVByUserEmail(authentication.getName())
        );
    }
}

