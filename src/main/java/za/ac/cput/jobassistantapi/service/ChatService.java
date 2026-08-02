package za.ac.cput.jobassistantapi.service;

import za.ac.cput.jobassistantapi.dto.request.ChatRequest;
import za.ac.cput.jobassistantapi.dto.response.ChatResponse;

import java.util.List;

public interface ChatService {

    ChatResponse sendMessage(ChatRequest request, String email);

    List<ChatResponse> getHistory(String email);
}
