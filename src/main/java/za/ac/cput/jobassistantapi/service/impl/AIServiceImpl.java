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

    private <T> T callGemini(Map<String, Object> requestBody, Class<T> responseType) throws Exception {
        String response = webClient.post()
                .uri("/v1beta/models/" + model + ":generateContent")
                .header("x-goog-api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response);

        String text = root
                .path("candidates").get(0)
                .path("content")
                .path("parts").get(0)
                .path("text")
                .asText();

        String cleanJson = text.replaceAll("```json", "").replaceAll("```", "").trim();

        return mapper.readValue(cleanJson, responseType);
    }
}