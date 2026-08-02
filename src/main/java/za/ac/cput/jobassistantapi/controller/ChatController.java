package za.ac.cput.jobassistantapi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import za.ac.cput.jobassistantapi.dto.request.ChatRequest;
import za.ac.cput.jobassistantapi.dto.response.ChatResponse;
import za.ac.cput.jobassistantapi.service.ChatService;

import java.util.List;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ResponseEntity<ChatResponse> sendMessage(
            @RequestBody ChatRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                chatService.sendMessage(request, authentication.getName())
        );
    }

    @GetMapping("/history")
    public ResponseEntity<List<ChatResponse>> getHistory(Authentication authentication) {
        return ResponseEntity.ok(
                chatService.getHistory(authentication.getName())
        );
    }
}
