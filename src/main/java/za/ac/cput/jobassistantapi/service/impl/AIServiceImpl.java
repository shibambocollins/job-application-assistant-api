package za.ac.cput.jobassistantapi.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import za.ac.cput.jobassistantapi.dto.response.CVDataResult;
import za.ac.cput.jobassistantapi.dto.response.JobFitResult;
import za.ac.cput.jobassistantapi.service.AIService;

import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class AIServiceImpl implements AIService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.model}")
    private String model;

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://generativelanguage.googleapis.com")
            .build();

    @Override
    public CVDataResult extractCVData(String cvText) {

        String prompt = """
            Extract structured data from this CV text.
            Return ONLY valid JSON, no markdown, no explanation, in this exact shape:
            {
              "skills": ["skill1", "skill2"],
              "education": ["degree, institution, year"],
              "certifications": ["cert name"],
              "projects": ["project name and short description"],
              "experience": ["role, company, duration"]
            }

            CV text:
            %s
            """.formatted(cvText);

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                )
        );

        try {
            return callGemini(requestBody, CVDataResult.class);
        } catch (Exception e) {
            throw new RuntimeException("AI extraction failed: " + e.getMessage());
        }
    }

    @Override
    public JobFitResult analyzeJobFit(String cvText, String jobDescription) {

        String prompt = """
            Compare this CV against this job description and assess fit.
            Return ONLY valid JSON, no markdown, no explanation, in this exact shape:
            {
              "matchScore": 0,
              "missingSkills": ["skill the job needs that the CV lacks"],
              "strengths": ["relevant CV strength for this job"],
              "suggestions": ["concrete improvement suggestion"]
            }
            matchScore is an integer from 0 to 100 estimating how well the CV fits the job.

            CV text:
            %s

            Job description:
            %s
            """.formatted(cvText, jobDescription);

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                )
        );

        try {
            return callGemini(requestBody, JobFitResult.class);
        } catch (Exception e) {
            throw new RuntimeException("AI job-fit analysis failed: " + e.getMessage());
        }
    }

    @Override
    public String generateChatReply(String contextSnapshot, String conversationHistory, String userMessage) {

        String prompt = """
            You are a helpful AI career assistant inside a job-search app called AI Job Assistant.
            Use the context below (the user's CV summary and tracked job applications) to answer
            their question directly and concisely. If the context doesn't have enough information
            to answer, say so honestly instead of guessing. Do not use markdown formatting.

            User context:
            %s

            Conversation so far:
            %s

            User: %s
            """.formatted(contextSnapshot, conversationHistory, userMessage);

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                )
        );

        try {
            return callGeminiText(requestBody);
        } catch (Exception e) {
            throw new RuntimeException("AI chat failed: " + e.getMessage());
        }
    }

    @Override
    public String extractTextFromImage(byte[] imageBytes, String mimeType) {

        String prompt = "Extract all readable text from this image, such as a CV/resume. " +
                "Return ONLY the raw extracted text, no commentary, no markdown formatting.";

        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt),
                                Map.of("inline_data", Map.of(
                                        "mime_type", mimeType,
                                        "data", base64Image
                                ))
                        ))
                )
        );

        try {
            return callGeminiText(requestBody);
        } catch (Exception e) {
            throw new RuntimeException("AI image text extraction failed: " + e.getMessage());
        }
    }

    private <T> T callGemini(Map<String, Object> requestBody, Class<T> responseType) throws Exception {
        String text = callGeminiText(requestBody);
        String cleanJson = text.replaceAll("```json", "").replaceAll("```", "").trim();

        return new ObjectMapper().readValue(cleanJson, responseType);
    }

    private String callGeminiText(Map<String, Object> requestBody) {
        String response = webClient.post()
                .uri("/v1beta/models/" + model + ":generateContent")
                .header("x-goog-api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(60))
                .block();

        try {
            JsonNode root = new ObjectMapper().readTree(response);

            return root
                    .path("candidates").get(0)
                    .path("content")
                    .path("parts").get(0)
                    .path("text")
                    .asText()
                    .trim();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Gemini response: " + e.getMessage());
        }
    }
}