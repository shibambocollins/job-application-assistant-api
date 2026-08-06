package za.ac.cput.jobassistantapi.dto.response;

import za.ac.cput.jobassistantapi.model.enums.ApplicationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class JobApplicationResponse {

    private Long id;
    private String jobTitle;
    private String company;
    private String location;
    private String postingUrl;
    private ApplicationStatus status;
    private LocalDate appliedDate;
    private LocalDateTime createdAt;

    public JobApplicationResponse(Long id, String jobTitle, String company, String location, String postingUrl,
                                  ApplicationStatus status, LocalDate appliedDate, LocalDateTime createdAt) {
        this.id = id;
        this.jobTitle = jobTitle;
        this.company = company;
        this.location = location;
        this.postingUrl = postingUrl;
        this.status = status;
        this.appliedDate = appliedDate;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public String getJobTitle() { return jobTitle; }
    public String getCompany() { return company; }
    public String getLocation() { return location; }
    public String getPostingUrl() { return postingUrl; }
    public ApplicationStatus getStatus() { return status; }
    public LocalDate getAppliedDate() { return appliedDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}