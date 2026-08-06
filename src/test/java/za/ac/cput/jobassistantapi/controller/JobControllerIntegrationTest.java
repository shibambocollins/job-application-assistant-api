package za.ac.cput.jobassistantapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Real HTTP + security + JPA(H2) stack, no mocked service layer. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class JobControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private String token;

    @BeforeEach
    void registerUser() throws Exception {
        String email = "jobtest-" + UUID.randomUUID() + "@example.com";
        String ip = "10.1." + (int) (Math.random() * 255) + "." + (int) (Math.random() * 255);

        MvcResult result = mockMvc.perform(post("/auth/register")
                        .header("X-Forwarded-For", ip)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email, "password", "TestPassword123", "fullName", "Job Tester"))))
                .andExpect(status().isOk())
                .andReturn();

        Map<?, ?> body = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        token = (String) body.get("token");
    }

    private Map<String, String> jobPayload(String postingUrl) {
        Map<String, String> payload = new HashMap<>();
        payload.put("title", "Junior Backend Developer");
        payload.put("company", "Acme Corp");
        payload.put("description", "Build things.");
        payload.put("location", "Remote");
        if (postingUrl != null) payload.put("postingUrl", postingUrl);
        return payload;
    }

    @Test
    void addJob_roundTripsPostingUrl() throws Exception {
        mockMvc.perform(post("/jobs")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(jobPayload("https://acme.example.com/careers/123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postingUrl").value("https://acme.example.com/careers/123"))
                .andExpect(jsonPath("$.jobTitle").value("Junior Backend Developer"))
                .andExpect(jsonPath("$.status").value("SAVED"));
    }

    @Test
    void addJob_withoutPostingUrl_returnsNullNotError() throws Exception {
        mockMvc.perform(post("/jobs")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(jobPayload(null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postingUrl").doesNotExist());
    }

    @Test
    void getMyApplications_afterAdding_includesTheJob() throws Exception {
        mockMvc.perform(post("/jobs")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(jobPayload("https://acme.example.com/careers/456"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/jobs").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].postingUrl").value("https://acme.example.com/careers/456"));
    }

    @Test
    void updateJobDetails_changesPostingUrl() throws Exception {
        MvcResult created = mockMvc.perform(post("/jobs")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(jobPayload("https://old.example.com"))))
                .andExpect(status().isOk())
                .andReturn();
        Map<?, ?> createdBody = objectMapper.readValue(created.getResponse().getContentAsString(), Map.class);
        Object id = createdBody.get("id");

        mockMvc.perform(put("/jobs/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(jobPayload("https://new.example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postingUrl").value("https://new.example.com"));
    }

    @Test
    void updateStatus_changesStatus() throws Exception {
        MvcResult created = mockMvc.perform(post("/jobs")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(jobPayload(null))))
                .andExpect(status().isOk())
                .andReturn();
        Map<?, ?> createdBody = objectMapper.readValue(created.getResponse().getContentAsString(), Map.class);
        Object id = createdBody.get("id");

        mockMvc.perform(patch("/jobs/" + id + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("status", "APPLIED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPLIED"));
    }

    @Test
    void deleteApplication_removesIt() throws Exception {
        MvcResult created = mockMvc.perform(post("/jobs")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(jobPayload(null))))
                .andExpect(status().isOk())
                .andReturn();
        Map<?, ?> createdBody = objectMapper.readValue(created.getResponse().getContentAsString(), Map.class);
        Object id = createdBody.get("id");

        mockMvc.perform(delete("/jobs/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        MvcResult afterDelete = mockMvc.perform(get("/jobs").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals("[]", afterDelete.getResponse().getContentAsString());
    }

    @Test
    void jobsEndpoint_withoutAuth_isForbidden() throws Exception {
        mockMvc.perform(get("/jobs")).andExpect(status().isForbidden());
    }
}
