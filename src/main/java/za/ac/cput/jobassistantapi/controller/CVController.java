package za.ac.cput.jobassistantapi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.jobassistantapi.dto.request.CVUploadRequest;
import za.ac.cput.jobassistantapi.dto.response.CVResponse;
import za.ac.cput.jobassistantapi.dto.response.CVUploadResponse;
import za.ac.cput.jobassistantapi.service.CVService;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/cv")
public class CVController {

    private final CVService cvService;

    public CVController(CVService cvService) {
        this.cvService = cvService;
    }

    @PostMapping
    public ResponseEntity<CVResponse> uploadCV(
            @RequestBody CVUploadRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                cvService.uploadCV(request, authentication.getName())
        );
    }

    @PostMapping("/upload")
    public ResponseEntity<CVUploadResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                cvService.uploadFile(
                        file,
                        authentication.getName()
                )
        );
    }
}