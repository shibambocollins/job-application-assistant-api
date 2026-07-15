package za.ac.cput.jobassistantapi.service.impl;

import org.springframework.stereotype.Service;
import za.ac.cput.jobassistantapi.dto.request.JobCreateRequest;
import za.ac.cput.jobassistantapi.dto.response.JobApplicationResponse;
import za.ac.cput.jobassistantapi.model.Job;
import za.ac.cput.jobassistantapi.model.JobApplication;
import za.ac.cput.jobassistantapi.model.User;
import za.ac.cput.jobassistantapi.model.enums.ApplicationStatus;
import za.ac.cput.jobassistantapi.model.enums.JobSource;
import za.ac.cput.jobassistantapi.repository.JobApplicationRepository;
import za.ac.cput.jobassistantapi.repository.JobRepository;
import za.ac.cput.jobassistantapi.repository.UserRepository;
import za.ac.cput.jobassistantapi.service.JobService;

import java.time.LocalDate;
import java.util.List;

@Service
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final UserRepository userRepository;

    public JobServiceImpl(JobRepository jobRepository,
                          JobApplicationRepository jobApplicationRepository,
                          UserRepository userRepository) {
        this.jobRepository = jobRepository;
        this.jobApplicationRepository = jobApplicationRepository;
        this.userRepository = userRepository;
    }

    @Override
    public JobApplicationResponse addManualJob(JobCreateRequest request, String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Job job = new Job.Builder()
                .setTitle(request.getTitle())
                .setCompany(request.getCompany())
                .setDescription(request.getDescription())
                .setLocation(request.getLocation())
                .setSource(JobSource.MANUAL)
                .build();

        Job savedJob = jobRepository.save(job);

        JobApplication application = new JobApplication.Builder()
                .setUser(user)
                .setJob(savedJob)
                .setStatus(ApplicationStatus.SAVED)
                .setAppliedDate(LocalDate.now())
                .build();

        JobApplication savedApp = jobApplicationRepository.save(application);

        return toResponse(savedApp);
    }

    @Override
    public List<JobApplicationResponse> getMyApplications(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return jobApplicationRepository.findByUserId(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public JobApplicationResponse updateStatus(Long applicationId, ApplicationStatus status, String email) {

        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        if (!application.getUser().getEmail().equals(email)) {
            throw new RuntimeException("Not your application");
        }

        JobApplication updated = new JobApplication.Builder()
                .copy(application)
                .setStatus(status)
                .build();

        JobApplication saved = jobApplicationRepository.save(updated);

        return toResponse(saved);
    }

    @Override
    public void deleteApplication(Long applicationId, String email) {

        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        if (!application.getUser().getEmail().equals(email)) {
            throw new RuntimeException("Not your application");
        }

        jobApplicationRepository.delete(application);
    }

    private JobApplicationResponse toResponse(JobApplication app) {
        return new JobApplicationResponse(
                app.getId(),
                app.getJob().getTitle(),
                app.getJob().getCompany(),
                app.getJob().getLocation(),
                app.getStatus(),
                app.getAppliedDate(),
                app.getCreatedAt()
        );
    }
}