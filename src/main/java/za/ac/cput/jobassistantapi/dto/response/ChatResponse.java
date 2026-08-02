package za.ac.cput.jobassistantapi.dto.response;

import java.time.LocalDateTime;

public class ChatResponse {

    private Long id;
    private String userMessage;
    private String aiResponse;
    private LocalDateTime sentAt;

    public ChatResponse(Long id, String userMessage, String aiResponse, LocalDateTime sentAt) {
        this.id = id;
        this.userMessage = userMessage;
        this.aiResponse = aiResponse;
        this.sentAt = sentAt;
    }

    public Long getId() { return id; }
    public String getUserMessage() { return userMessage; }
    public String getAiResponse() { return aiResponse; }
    public LocalDateTime getSentAt() { return sentAt; }
}
