package za.ac.cput.jobassistantapi.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public class AnalysisResponse {

    private Long id;
    private Long jobApplicationId;
    private String jobTitle;
    private String company;
    private Integer matchScore;
    private List<String> missingSkills;
    private List<String> strengths;
    private List<String> suggestions;
    private LocalDateTime createdAt;

    public AnalysisResponse(Long id, Long jobApplicationId, String jobTitle, String company,
                            Integer matchScore, List<String> missingSkills,
                            List<String> strengths, List<String> suggestions,
                            LocalDateTime createdAt) {
        this.id = id;
        this.jobApplicationId = jobApplicationId;
        this.jobTitle = jobTitle;
        this.company = company;
        this.matchScore = matchScore;
        this.missingSkills = missingSkills;
        this.strengths = strengths;
        this.suggestions = suggestions;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Long getJobApplicationId() { return jobApplicationId; }
    public String getJobTitle() { return jobTitle; }
    public String getCompany() { return company; }
    public Integer getMatchScore() { return matchScore; }
    public List<String> getMissingSkills() { return missingSkills; }
    public List<String> getStrengths() { return strengths; }
    public List<String> getSuggestions() { return suggestions; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
