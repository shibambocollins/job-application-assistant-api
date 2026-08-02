package za.ac.cput.jobassistantapi.dto.response;

import java.util.List;

public class JobFitResult {

    private Integer matchScore;
    private List<String> missingSkills;
    private List<String> strengths;
    private List<String> suggestions;

    public JobFitResult() {}

    public JobFitResult(Integer matchScore, List<String> missingSkills,
                        List<String> strengths, List<String> suggestions) {
        this.matchScore = matchScore;
        this.missingSkills = missingSkills;
        this.strengths = strengths;
        this.suggestions = suggestions;
    }

    public Integer getMatchScore() { return matchScore; }
    public List<String> getMissingSkills() { return missingSkills; }
    public List<String> getStrengths() { return strengths; }
    public List<String> getSuggestions() { return suggestions; }

    public void setMatchScore(Integer matchScore) { this.matchScore = matchScore; }
    public void setMissingSkills(List<String> missingSkills) { this.missingSkills = missingSkills; }
    public void setStrengths(List<String> strengths) { this.strengths = strengths; }
    public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }
}
