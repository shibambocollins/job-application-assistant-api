package za.ac.cput.jobassistantapi.dto.response;

import java.util.List;

public class CVDataResult {

    private List<String> skills;
    private List<String> education;
    private List<String> certifications;
    private List<String> projects;
    private List<String> experience;

    public CVDataResult() {}

    public CVDataResult(List<String> skills, List<String> education,
                        List<String> certifications, List<String> projects,
                        List<String> experience) {
        this.skills = skills;
        this.education = education;
        this.certifications = certifications;
        this.projects = projects;
        this.experience = experience;
    }

    public List<String> getSkills() { return skills; }
    public List<String> getEducation() { return education; }
    public List<String> getCertifications() { return certifications; }
    public List<String> getProjects() { return projects; }
    public List<String> getExperience() { return experience; }

    public void setSkills(List<String> skills) { this.skills = skills; }
    public void setEducation(List<String> education) { this.education = education; }
    public void setCertifications(List<String> certifications) { this.certifications = certifications; }
    public void setProjects(List<String> projects) { this.projects = projects; }
    public void setExperience(List<String> experience) { this.experience = experience; }
}