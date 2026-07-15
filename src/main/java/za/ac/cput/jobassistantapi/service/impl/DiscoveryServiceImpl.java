package za.ac.cput.jobassistantapi.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import za.ac.cput.jobassistantapi.dto.response.JobApplicationResponse;
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
                .orElseThrow(() -> new RuntimeException("User not found"));

        CV cv = cvRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Upload a CV first"));

        // Hardcoded category — CV skills are dev-focused, so this always applies for your app
        List<JsonNode> museJobs = museApiClient.fetchJobs("Software Engineering", "Entry Level");

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
                        saved.getStatus(),
                        saved.getAppliedDate(),
                        saved.getCreatedAt()
                ));
            }
        }

        return results;
    }
}