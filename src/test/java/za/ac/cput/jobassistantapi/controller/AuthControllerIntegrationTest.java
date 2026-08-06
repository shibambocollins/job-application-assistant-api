package za.ac.cput.jobassistantapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the real HTTP + Spring Security + JPA(H2) stack end to end, rather than mocking the
 * service layer. Every test uses its own synthetic client IP (via X-Forwarded-For) so the
 * IP-keyed rate limiters can't leak state between tests regardless of execution order.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private String uniqueIp() {
        return "10.0." + (int) (Math.random() * 255) + "." + (int) (Math.random() * 255);
    }

    private String uniqueEmail() {
        return "itest-" + UUID.randomUUID() + "@example.com";
    }

    private String register(String email, String password, String fullName, String ip) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/register")
                        .header("X-Forwarded-For", ip)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email, "password", password, "fullName", fullName))))
                .andExpect(status().isOk())
                .andReturn();
        Map<?, ?> body = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        return (String) body.get("token");
    }

    @Test
    void register_thenLogin_returnsToken() throws Exception {
        String email = uniqueEmail();
        String ip = uniqueIp();

        register(email, "TestPassword123", "Test User", ip);

        mockMvc.perform(post("/auth/login")
                        .header("X-Forwarded-For", uniqueIp())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", "TestPassword123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    void login_wrongPassword_returns401WithSpecificMessage() throws Exception {
        String email = uniqueEmail();
        register(email, "TestPassword123", "Test User", uniqueIp());

        mockMvc.perform(post("/auth/login")
                        .header("X-Forwarded-For", uniqueIp())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", "WrongPassword"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid password"));
    }

    @Test
    void login_unknownEmail_returns404WithSpecificMessage() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .header("X-Forwarded-For", uniqueIp())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("email", uniqueEmail(), "password", "whatever123"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    @Test
    void me_withoutToken_isUnauthorized() throws Exception {
        mockMvc.perform(get("/auth/me").header("X-Forwarded-For", uniqueIp()))
                .andExpect(status().isForbidden());
    }

    @Test
    void me_withValidToken_returnsProfile() throws Exception {
        String email = uniqueEmail();
        String token = register(email, "TestPassword123", "Profile Person", uniqueIp());

        mockMvc.perform(get("/auth/me")
                        .header("X-Forwarded-For", uniqueIp())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.fullName").value("Profile Person"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void changePassword_thenLoginWithNewPassword_succeeds() throws Exception {
        String email = uniqueEmail();
        String token = register(email, "OldPassword123", "Test User", uniqueIp());

        mockMvc.perform(post("/auth/change-password")
                        .header("X-Forwarded-For", uniqueIp())
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "currentPassword", "OldPassword123", "newPassword", "NewPassword456"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/auth/login")
                        .header("X-Forwarded-For", uniqueIp())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", "NewPassword456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    void changePassword_wrongCurrentPassword_returns401() throws Exception {
        String token = register(uniqueEmail(), "OldPassword123", "Test User", uniqueIp());

        mockMvc.perform(post("/auth/change-password")
                        .header("X-Forwarded-For", uniqueIp())
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "currentPassword", "WrongPassword", "newPassword", "NewPassword456"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteAccount_thenLogin_userNoLongerExists() throws Exception {
        String email = uniqueEmail();
        String token = register(email, "TestPassword123", "Test User", uniqueIp());

        mockMvc.perform(delete("/auth/me")
                        .header("X-Forwarded-For", uniqueIp())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/auth/login")
                        .header("X-Forwarded-For", uniqueIp())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", "TestPassword123"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        String email = uniqueEmail();
        register(email, "TestPassword123", "Test User", uniqueIp());

        mockMvc.perform(post("/auth/register")
                        .header("X-Forwarded-For", uniqueIp())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email, "password", "TestPassword123", "fullName", "Duplicate"))))
                .andExpect(status().isConflict());
    }

    @Test
    void register_repeatedFromSameIp_eventuallyRateLimited() throws Exception {
        String ip = uniqueIp();

        // Registration is capped at 5/hour per IP; the 6th attempt from the same IP should 429.
        for (int i = 0; i < 5; i++) {
            register(uniqueEmail(), "TestPassword123", "Rate Limit Test", ip);
        }

        mockMvc.perform(post("/auth/register")
                        .header("X-Forwarded-For", ip)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", uniqueEmail(), "password", "TestPassword123", "fullName", "One Too Many"))))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void register_sameIpDifferentSourcePorts_shareRateLimitBucket() throws Exception {
        // Azure's front door appends the client's ephemeral source port to X-Forwarded-For
        // ("203.0.113.9:54321"), which differs on every connection from the same client. The
        // limiter must key on the IP only, or every request looks like a distinct client.
        String ip = uniqueIp();

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/auth/register")
                            .header("X-Forwarded-For", ip + ":" + (40000 + i))
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "email", uniqueEmail(), "password", "TestPassword123", "fullName", "Port Test"))))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(post("/auth/register")
                        .header("X-Forwarded-For", ip + ":49999")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", uniqueEmail(), "password", "TestPassword123", "fullName", "One Too Many"))))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void login_repeatedWrongPassword_eventuallyLocksOut() throws Exception {
        String email = uniqueEmail();
        register(email, "CorrectPassword123", "Lockout Test", uniqueIp());

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/auth/login")
                    .header("X-Forwarded-For", uniqueIp())
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(Map.of("email", email, "password", "WrongPassword"))));
        }

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .header("X-Forwarded-For", uniqueIp())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", "CorrectPassword123"))))
                .andReturn();

        assertEquals(429, result.getResponse().getStatus());
    }

    @Test
    void forgotPassword_unknownEmail_stillReturns200() throws Exception {
        mockMvc.perform(post("/auth/forgot-password")
                        .header("X-Forwarded-For", uniqueIp())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("email", uniqueEmail()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void resetPassword_invalidToken_returns400() throws Exception {
        mockMvc.perform(post("/auth/reset-password")
                        .header("X-Forwarded-For", uniqueIp())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "token", "does-not-exist", "newPassword", "NewPassword456"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_weakPasswordTooShort_returns400() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .header("X-Forwarded-For", uniqueIp())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", uniqueEmail(), "password", "short", "fullName", "Test"))))
                .andExpect(status().isBadRequest());
    }
}
