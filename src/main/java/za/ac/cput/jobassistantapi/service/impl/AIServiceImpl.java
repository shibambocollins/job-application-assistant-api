package za.ac.cput.jobassistantapi.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import za.ac.cput.jobassistantapi.dto.response.CVDataResult;
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

        // Build the full path manually to avoid the {model}: colon parsing bug
        String path = "/v1beta/models/" + model + ":generateContent?key=" + apiKey;

        System.out.println("DEBUG FULL URL: https://generativelanguage.googleapis.com" + path);

        try {
            String response = webClient.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return parseGeminiResponse(response);

        } catch (Exception e) {
            throw new RuntimeException("AI extraction failed: " + e.getMessage());
        }
    }

    private CVDataResult parseGeminiResponse(String rawResponse) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(rawResponse);

        String text = root
                .path("candidates").get(0)
                .path("content")
                .path("parts").get(0)
                .path("text")
                .asText();

        String cleanJson = text.replaceAll("```json", "").replaceAll("```", "").trim();

        return mapper.readValue(cleanJson, CVDataResult.class);
    }
}