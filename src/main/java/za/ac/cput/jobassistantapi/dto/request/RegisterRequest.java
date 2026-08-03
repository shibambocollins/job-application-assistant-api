package za.ac.cput.jobassistantapi.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank(message = "email is required")
    @Email(message = "must be a valid email address")
    private String email;

    @NotBlank(message = "password is required")
    @Size(min = 8, message = "must be at least 8 characters")
    private String password;

    @NotBlank(message = "fullName is required")
    private String fullName;

    public RegisterRequest() {}

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
}