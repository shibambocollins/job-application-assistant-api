package za.ac.cput.jobassistantapi.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import za.ac.cput.jobassistantapi.dto.request.ChatRequest;
import za.ac.cput.jobassistantapi.dto.response.ChatResponse;
import za.ac.cput.jobassistantapi.dto.response.CVDataResult;
import za.ac.cput.jobassistantapi.exception.ResourceNotFoundException;
import za.ac.cput.jobassistantapi.model.ChatMessage;
import za.ac.cput.jobassistantapi.model.JobApplication;
import za.ac.cput.jobassistantapi.model.User;
import za.ac.cput.jobassistantapi.repository.AnalysisRepository;
import za.ac.cput.jobassistantapi.repository.CVRepository;
import za.ac.cput.jobassistantapi.repository.ChatMessageRepository;
import za.ac.cput.jobassistantapi.repository.JobApplicationRepository;
import za.ac.cput.jobassistantapi.repository.UserRepository;
import za.ac.cput.jobassistantapi.service.AIService;
import za.ac.cput.jobassistantapi.service.ChatService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class ChatServiceImpl implements ChatService {

    private final UserRepository userRepository;
    private final CVRepository cvRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final AnalysisRepository analysisRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final AIService aiService;
    private final ObjectMapper mapper = new ObjectMapper();

    public ChatServiceImpl(UserRepository userRepository,
                           CVRepository cvRepository,
                           JobApplicationRepository jobApplicationRepository,
                           AnalysisRepository analysisRepository,
                           ChatMessageRepository chatMessageRepository,
                           AIService aiService) {
        this.userRepository = userRepository;
        this.cvRepository = cvRepository;
        this.jobApplicationRepository = jobApplicationRepository;
        this.analysisRepository = analysisRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.aiService = aiService;
    }

    @Override
    public ChatResponse sendMessage(ChatRequest request, String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String contextSnapshot = buildContextSnapshot(user);
        String conversationHistory = buildConversationHistory(user.getId());

        String aiResponse = aiService.generateChatReply(contextSnapshot, conversationHistory, request.getMessage());

        ChatMessage message = new ChatMessage.Builder()
                .setUser(user)
                .setUserMessage(request.getMessage())
                .setAiResponse(aiResponse)
                .setContextSnapshot(contextSnapshot)
                .build();

        ChatMessage saved = chatMessageRepository.save(message);

        return toResponse(saved);
    }

    @Override
    public List<ChatResponse> getHistory(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return chatMessageRepository.findByUser_IdOrderBySentAtAsc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private String buildContextSnapshot(User user) {
        StringBuilder sb = new StringBuilder();

        cvRepository.findByUserId(user.getId()).ifPresentOrElse(cv -> {
            try {
                CVDataResult cvData = mapper.readValue(cv.getSkillsJson(), CVDataResult.class);
                sb.append("CV skills: ").append(String.join(", ", cvData.getSkills())).append("\n");
                sb.append("Education: ").append(String.join(", ", cvData.getEducation())).append("\n");
                sb.append("Certifications: ").append(String.join(", ", cvData.getCertifications())).append("\n");
            } catch (Exception e) {
                sb.append("User has a CV uploaded but its skills could not be parsed.\n");
            }
        }, () -> sb.append("User has not uploaded a CV yet.\n"));

        List<JobApplication> applications = jobApplicationRepository.findByUserId(user.getId());

        if (applications.isEmpty()) {
            sb.append("No tracked job applications yet.\n");
        } else {
            sb.append("Tracked job applications:\n");
            for (JobApplication app : applications) {
                sb.append("- ").append(app.getJob().getTitle())
                        .append(" at ").append(app.getJob().getCompany())
                        .append(" (status: ").append(app.getStatus()).append(")");

                analysisRepository.findTopByJobApplication_IdOrderByCreatedAtDesc(app.getId())
                        .ifPresent(a -> {
                            sb.append(", match score: ").append(a.getMatchScore()).append("%");
                            List<String> missingSkills = parseSkillsList(a.getMissingSkills());
                            if (!missingSkills.isEmpty()) {
                                sb.append(", missing skills: ").append(String.join(", ", missingSkills));
                            }
                        });

                sb.append("\n");
            }
        }

        return sb.toString();
    }

    private List<String> parseSkillsList(String json) {
        try {
            return mapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private String buildConversationHistory(Long userId) {
        List<ChatMessage> recent = new ArrayList<>(
                chatMessageRepository.findTop10ByUser_IdOrderBySentAtDesc(userId)
        );
        Collections.reverse(recent);

        if (recent.isEmpty()) {
            return "(no previous conversation)";
        }

        StringBuilder sb = new StringBuilder();
        for (ChatMessage m : recent) {
            sb.append("User: ").append(m.getUserMessage()).append("\n");
            sb.append("Assistant: ").append(m.getAiResponse()).append("\n");
        }
        return sb.toString();
    }

    private ChatResponse toResponse(ChatMessage message) {
        return new ChatResponse(
                message.getId(),
                message.getUserMessage(),
                message.getAiResponse(),
                message.getSentAt()
        );
    }
}
