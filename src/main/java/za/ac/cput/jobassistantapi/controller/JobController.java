package za.ac.cput.jobassistantapi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.jobassistantapi.dto.request.JobCreateRequest;
import za.ac.cput.jobassistantapi.dto.request.StatusUpdateRequest;
import za.ac.cput.jobassistantapi.dto.response.JobApplicationResponse;
import za.ac.cput.jobassistantapi.service.JobService;

import java.util.List;

@RestController
@RequestMapping("/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping
    public ResponseEntity<JobApplicationResponse> addJob(
            @RequestBody JobCreateRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                jobService.addManualJob(request, authentication.getName())
        );
    }

    @GetMapping
    public ResponseEntity<List<JobApplicationResponse>> getMyApplications(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                jobService.getMyApplications(authentication.getName())
        );
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<JobApplicationResponse> updateStatus(
            @PathVariable Long id,
            @RequestBody StatusUpdateRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                jobService.updateStatus(id, request.getStatus(), authentication.getName())
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApplication(
            @PathVariable Long id,
            Authentication authentication
    ) {
        jobService.deleteApplication(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}