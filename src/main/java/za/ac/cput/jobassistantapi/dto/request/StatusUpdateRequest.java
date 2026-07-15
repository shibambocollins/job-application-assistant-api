package za.ac.cput.jobassistantapi.dto.request;

import za.ac.cput.jobassistantapi.model.enums.ApplicationStatus;

public class StatusUpdateRequest {

    private ApplicationStatus status;

    public StatusUpdateRequest() {}

    public ApplicationStatus getStatus() { return status; }
    public void setStatus(ApplicationStatus status) { this.status = status; }
}