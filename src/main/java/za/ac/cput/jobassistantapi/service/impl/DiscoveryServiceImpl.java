package za.ac.cput.jobassistantapi.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import za.ac.cput.jobassistantapi.dto.response.JobApplicationResponse;
import za.ac.cput.jobassistantapi.exception.ResourceNotFoundException;
import za.ac.cput.jobassistantapi.model.CV;
import za.ac.cput.jobassistantapi.model.Job;
import za.ac.cput.jobassistantapi.model.JobApplication;
import za.ac.cput.jobassistantapi.model.User;
import za.ac.cput.jobassistantapi.model.enums.ApplicationStatus;
import za.ac.cput.jobassistantapi.model.enums.JobSource;
import za.ac.cput.jobassistantapi.repository.CVRepository;
import za.ac.cput.jobassistantapi.repository.JobApplicationRepository;
import za.ac.cput.jobassistantapi.repository.JobRepository;
import za.ac.cput.jobassistantapi.repository.UserRepository;
import za.ac.cput.jobassistantapi.service.DiscoveryService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class DiscoveryServiceImpl implements DiscoveryService {

    // Broad tech coverage rather than software-engineering-only, so the discovery feature
    // is useful to more than just backend/frontend dev candidates.
    private static final List<String> TECH_CATEGORIES = List.of(
            "Software Engineering",
            "Data and Analytics",
            "Science and Engineering",
            "Design and UX"
    );

    private final MuseApiClient museApiClient;
    private final JobRepository jobRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final UserRepository userRepository;
    private final CVRepository cvRepository;

    public DiscoveryServiceImpl(MuseApiClient museApiClient,
                                JobRepository jobRepository,
                                JobApplicationRepository jobApplicationRepository,
                                UserRepository userRepository,
                                CVRepository cvRepository) {
        this.museApiClient = museApiClient;
        this.jobRepository = jobRepository;
        this.jobApplicationRepository = jobApplicationRepository;
        this.userRepository = userRepository;
        this.cvRepository = cvRepository;
    }

    @Override
    public List<JobApplicationResponse> discoverJobs(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        cvRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Upload a CV first"));

        List<JsonNode> museJobs = museApiClient.fetchJobs(TECH_CATEGORIES, "Entry Level");

        List<JobApplicationResponse> results = new ArrayList<>();

        for (JsonNode museJob : museJobs) {

            String externalId = museJob.path("id").asText();

            Job job = jobRepository.findByExternalId(externalId)
                    .orElseGet(() -> {
                        Job newJob = new Job.Builder()
                                .setExternalId(externalId)
                                .setTitle(museJob.path("name").asText())
                                .setCompany(museJob.path("company").path("name").asText())
                                .setDescription(museJob.path("contents").asText())
                                .setLocation(
                                        museJob.path("locations").isArray() && museJob.path("locations").size() > 0
                                                ? museJob.path("locations").get(0).path("name").asText()
                                                : "Not specified"
                                )
                                .setPostingUrl(museJob.path("refs").path("landing_page").asText(null))
                                .setSource(JobSource.MUSE)
                                .build();
                        return jobRepository.save(newJob);
                    });

            if (!jobApplicationRepository.existsByUserIdAndJobId(user.getId(), job.getId())) {

                JobApplication application = new JobApplication.Builder()
                        .setUser(user)
                        .setJob(job)
                        .setStatus(ApplicationStatus.SAVED)
                        .setAppliedDate(LocalDate.now())
                        .build();

                JobApplication saved = jobApplicationRepository.save(application);

                results.add(new JobApplicationResponse(
                        saved.getId(),
                        job.getTitle(),
                        job.getCompany(),
                        job.getLocation(),
                        job.getPostingUrl(),
                        saved.getStatus(),
                        saved.getAppliedDate(),
                        saved.getCreatedAt()
                ));
            }
        }

        return results;
    }
}
