package za.ac.cput.jobassistantapi.dto.request;

public class ChatRequest {

    private String message;

    public ChatRequest() {}

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
