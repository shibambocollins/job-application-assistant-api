package za.ac.cput.jobassistantapi.dto.request;

import jakarta.validation.constraints.NotBlank;

public class JobCreateRequest {

    @NotBlank(message = "title is required")
    private String title;

    @NotBlank(message = "company is required")
    private String company;

    @NotBlank(message = "description is required")
    private String description;

    private String location;

    private String postingUrl;

    public JobCreateRequest() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getPostingUrl() { return postingUrl; }
    public void setPostingUrl(String postingUrl) { this.postingUrl = postingUrl; }
}