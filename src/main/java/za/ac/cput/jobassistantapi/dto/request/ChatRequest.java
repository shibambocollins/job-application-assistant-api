package za.ac.cput.jobassistantapi.dto.request;

import jakarta.validation.constraints.NotBlank;

public class ChatRequest {

    @NotBlank(message = "message is required")
    private String message;

    public ChatRequest() {}

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
