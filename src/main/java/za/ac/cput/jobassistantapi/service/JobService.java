package za.ac.cput.jobassistantapi.service;

import za.ac.cput.jobassistantapi.dto.request.JobCreateRequest;
import za.ac.cput.jobassistantapi.dto.response.JobApplicationResponse;
import za.ac.cput.jobassistantapi.model.enums.ApplicationStatus;

import java.util.List;

public interface JobService {

    JobApplicationResponse addManualJob(JobCreateRequest request, String email);

    List<JobApplicationResponse> getMyApplications(String email);

    JobApplicationResponse updateStatus(Long applicationId, ApplicationStatus status, String email);

    JobApplicationResponse updateJobDetails(Long applicationId, JobCreateRequest request, String email);

    void deleteApplication(Long applicationId, String email);
}