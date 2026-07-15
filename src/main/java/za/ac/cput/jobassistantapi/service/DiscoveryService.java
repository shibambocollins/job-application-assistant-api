package za.ac.cput.jobassistantapi.service;

import za.ac.cput.jobassistantapi.dto.response.JobApplicationResponse;

import java.util.List;

public interface DiscoveryService {
    List<JobApplicationResponse> discoverJobs(String email);
}