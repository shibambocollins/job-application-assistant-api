package za.ac.cput.jobassistantapi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.jobassistantapi.dto.response.AnalysisResponse;
import za.ac.cput.jobassistantapi.service.AnalysisService;

@RestController
@RequestMapping("/jobs/{id}/analysis")
public class AnalysisController {

    private final AnalysisService analysisService;

    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @PostMapping
    public ResponseEntity<AnalysisResponse> analyze(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                analysisService.analyzeJobApplication(id, authentication.getName())
        );
    }

    @GetMapping
    public ResponseEntity<AnalysisResponse> getLatest(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                analysisService.getLatestAnalysis(id, authentication.getName())
        );
    }
}
