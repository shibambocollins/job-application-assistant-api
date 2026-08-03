package za.ac.cput.jobassistantapi.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.jobassistantapi.dto.request.ChatRequest;
import za.ac.cput.jobassistantapi.dto.response.ChatResponse;
import za.ac.cput.jobassistantapi.exception.ResourceNotFoundException;
import za.ac.cput.jobassistantapi.model.Analysis;
import za.ac.cput.jobassistantapi.model.CV;
import za.ac.cput.jobassistantapi.model.ChatMessage;
import za.ac.cput.jobassistantapi.model.Job;
import za.ac.cput.jobassistantapi.model.JobApplication;
import za.ac.cput.jobassistantapi.model.User;
import za.ac.cput.jobassistantapi.model.enums.ApplicationStatus;
import za.ac.cput.jobassistantapi.repository.AnalysisRepository;
import za.ac.cput.jobassistantapi.repository.CVRepository;
import za.ac.cput.jobassistantapi.repository.ChatMessageRepository;
import za.ac.cput.jobassistantapi.repository.JobApplicationRepository;
import za.ac.cput.jobassistantapi.repository.UserRepository;
import za.ac.cput.jobassistantapi.service.AIService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private CVRepository cvRepository;
    @Mock private JobApplicationRepository jobApplicationRepository;
    @Mock private AnalysisRepository analysisRepository;
    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private AIService aiService;

    @InjectMocks
    private ChatServiceImpl chatService;

    private static final String EMAIL = "user@example.com";

    private User user() {
        return new User.Builder().setId(1L).setEmail(EMAIL).build();
    }

    private ChatRequest request(String message) {
        ChatRequest request = new ChatRequest();
        request.setMessage(message);
        return request;
    }

    @Test
    void sendMessage_noCvNoJobs_buildsMinimalContext() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user()));
        when(cvRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(jobApplicationRepository.findByUserId(1L)).thenReturn(List.of());
        when(chatMessageRepository.findTop10ByUser_IdOrderBySentAtDesc(1L)).thenReturn(List.of());

        ArgumentCaptor<String> contextCaptor = ArgumentCaptor.forClass(String.class);
        when(aiService.generateChatReply(contextCaptor.capture(), anyString(), eq("Hello")))
                .thenReturn("AI reply");

        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage m = invocation.getArgument(0);
            return new ChatMessage.Builder().copy(m).setId(1L).build();
        });

        ChatResponse response = chatService.sendMessage(request("Hello"), EMAIL);

        assertEquals("AI reply", response.getAiResponse());
        assertTrue(contextCaptor.getValue().contains("User has not uploaded a CV yet."));
        assertTrue(contextCaptor.getValue().contains("No tracked job applications yet."));
    }

    @Test
    void sendMessage_withCvAndJobs_includesSkillsAndMatchScoreInContext() {
        User user = user();
        CV cv = new CV.Builder().setId(1L).setUserId(1L)
                .setSkillsJson("{\"skills\":[\"Java\",\"Spring Boot\"],\"education\":[],\"certifications\":[],\"projects\":[],\"experience\":[]}")
                .build();

        Job job = new Job.Builder().setId(5L).setTitle("Junior Dev").setCompany("Acme").build();
        JobApplication app = new JobApplication.Builder()
                .setId(10L).setUser(user).setJob(job)
                .setStatus(ApplicationStatus.SAVED).setAppliedDate(LocalDate.now()).build();

        Analysis analysis = new Analysis.Builder()
                .setId(20L).setJobApplication(app).setCv(cv)
                .setMatchScore(65).setMissingSkills("[\"Docker\"]").setStrengths("[]").setAiSuggestions("[]")
                .build();

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(cvRepository.findByUserId(1L)).thenReturn(Optional.of(cv));
        when(jobApplicationRepository.findByUserId(1L)).thenReturn(List.of(app));
        when(analysisRepository.findTopByJobApplication_IdOrderByCreatedAtDesc(10L))
                .thenReturn(Optional.of(analysis));
        when(chatMessageRepository.findTop10ByUser_IdOrderBySentAtDesc(1L)).thenReturn(List.of());

        ArgumentCaptor<String> contextCaptor = ArgumentCaptor.forClass(String.class);
        when(aiService.generateChatReply(contextCaptor.capture(), anyString(), anyString()))
                .thenReturn("AI reply");

        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage m = invocation.getArgument(0);
            return new ChatMessage.Builder().copy(m).setId(2L).build();
        });

        chatService.sendMessage(request("What fits me best?"), EMAIL);

        String context = contextCaptor.getValue();
        assertTrue(context.contains("Java"));
        assertTrue(context.contains("Junior Dev"));
        assertTrue(context.contains("match score: 65%"));
        assertTrue(context.contains("missing skills: Docker"));
    }

    @Test
    void sendMessage_userNotFound_throwsResourceNotFoundException() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> chatService.sendMessage(request("Hi"), EMAIL));
    }

    @Test
    void getHistory_success_returnsOrderedMessages() {
        ChatMessage message = new ChatMessage.Builder()
                .setId(1L).setUser(user()).setUserMessage("Hi").setAiResponse("Hello").build();

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user()));
        when(chatMessageRepository.findByUser_IdOrderBySentAtAsc(1L)).thenReturn(List.of(message));

        List<ChatResponse> history = chatService.getHistory(EMAIL);

        assertEquals(1, history.size());
        assertEquals("Hi", history.get(0).getUserMessage());
    }

    @Test
    void getHistory_userNotFound_throwsResourceNotFoundException() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> chatService.getHistory(EMAIL));
    }
}
