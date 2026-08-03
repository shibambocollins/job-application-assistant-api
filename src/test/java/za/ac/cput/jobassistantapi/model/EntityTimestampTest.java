package za.ac.cput.jobassistantapi.model;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import za.ac.cput.jobassistantapi.model.enums.ApplicationStatus;
import za.ac.cput.jobassistantapi.model.enums.JobSource;
import za.ac.cput.jobassistantapi.repository.AnalysisRepository;
import za.ac.cput.jobassistantapi.repository.CVRepository;
import za.ac.cput.jobassistantapi.repository.ChatMessageRepository;
import za.ac.cput.jobassistantapi.repository.JobApplicationRepository;
import za.ac.cput.jobassistantapi.repository.JobRepository;
import za.ac.cput.jobassistantapi.repository.UserRepository;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Builder.build() alone never triggers @PrePersist, so timestamp auto-generation
 * can only be verified by actually persisting through a repository.
 */
@DataJpaTest
class EntityTimestampTest {

    @Autowired private UserRepository userRepository;
    @Autowired private CVRepository cvRepository;
    @Autowired private JobRepository jobRepository;
    @Autowired private JobApplicationRepository jobApplicationRepository;
    @Autowired private AnalysisRepository analysisRepository;
    @Autowired private ChatMessageRepository chatMessageRepository;

    @Test
    void user_createdAt_isSetOnPersist() {
        User saved = userRepository.save(new User.Builder()
                .setEmail("timestamp-user@example.com").setPasswordHash("hash").setFullName("Timestamp Test").build());

        assertNotNull(saved.getCreatedAt());
    }

    @Test
    void cv_uploadedAt_isSetOnPersist() {
        User user = userRepository.save(new User.Builder()
                .setEmail("cv-timestamp@example.com").setPasswordHash("hash").setFullName("CV Test").build());

        CV saved = cvRepository.save(new CV.Builder()
                .setUserId(user.getId())
                .setBlobUrl("https://blob.url/cv.pdf")
                .setOriginalFilename("cv.pdf")
                .setExtractedText("text")
                .setSkillsJson("{}")
                .build());

        assertNotNull(saved.getUploadedAt());
    }

    @Test
    void job_fetchedAt_isSetOnPersist() {
        Job saved = jobRepository.save(new Job.Builder()
                .setTitle("Dev").setCompany("Acme").setSource(JobSource.MANUAL).build());

        assertNotNull(saved.getFetchedAt());
    }

    @Test
    void jobApplication_createdAt_isSetOnPersist() {
        User user = userRepository.save(new User.Builder()
                .setEmail("app-timestamp@example.com").setPasswordHash("hash").setFullName("App Test").build());
        Job job = jobRepository.save(new Job.Builder()
                .setTitle("Dev").setCompany("Acme").setSource(JobSource.MANUAL).build());

        JobApplication saved = jobApplicationRepository.save(new JobApplication.Builder()
                .setUser(user).setJob(job).setStatus(ApplicationStatus.SAVED).setAppliedDate(LocalDate.now())
                .build());

        assertNotNull(saved.getCreatedAt());
    }

    @Test
    void analysis_createdAt_isSetOnPersist() {
        User user = userRepository.save(new User.Builder()
                .setEmail("analysis-timestamp@example.com").setPasswordHash("hash").setFullName("Analysis Test").build());
        Job job = jobRepository.save(new Job.Builder()
                .setTitle("Dev").setCompany("Acme").setSource(JobSource.MANUAL).build());
        JobApplication app = jobApplicationRepository.save(new JobApplication.Builder()
                .setUser(user).setJob(job).setStatus(ApplicationStatus.SAVED).setAppliedDate(LocalDate.now()).build());
        CV cv = cvRepository.save(new CV.Builder()
                .setUserId(user.getId()).setBlobUrl("url").setOriginalFilename("cv.pdf")
                .setExtractedText("text").setSkillsJson("{}").build());

        Analysis saved = analysisRepository.save(new Analysis.Builder()
                .setJobApplication(app).setCv(cv).setMatchScore(50)
                .setMissingSkills("[]").setStrengths("[]").setAiSuggestions("[]")
                .build());

        assertNotNull(saved.getCreatedAt());
    }

    @Test
    void chatMessage_sentAt_isSetOnPersist() {
        User user = userRepository.save(new User.Builder()
                .setEmail("chat-timestamp@example.com").setPasswordHash("hash").setFullName("Chat Test").build());

        ChatMessage saved = chatMessageRepository.save(new ChatMessage.Builder()
                .setUser(user).setUserMessage("Hi").setAiResponse("Hello").build());

        assertNotNull(saved.getSentAt());
    }
}
