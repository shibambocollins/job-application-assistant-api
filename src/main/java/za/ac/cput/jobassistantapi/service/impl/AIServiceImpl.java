package za.ac.cput.jobassistantapi.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(AIServiceImpl.class);

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
            Compare this CV against this job description and assess overall fit. Judge this holistically, not
            just on technical skills. Weigh all of the following:
            - Technical skills AND soft skills / core competencies the role calls for
            - Relevant work experience (roles, responsibilities, seniority, years, industry relevance)
            - Educational background, where the job specifies or implies a requirement
            - Location fit, if the job lists a location and the CV indicates a location or remote availability
            - Whether the CV's structure would parse cleanly through an Applicant Tracking System (ATS) —
              clear section headings, no tables/columns/text-boxes/graphics, standard fonts, consistent
              formatting. Flag structural issues here as suggestions if they'd hurt ATS parsing, separate from
              content gaps.

            Return ONLY valid JSON, no markdown, no explanation, in this exact shape:
            {
              "matchScore": 0,
              "missingSkills": ["a skill, experience gap, education gap, or competency the job needs that the CV lacks or doesn't clearly demonstrate"],
              "strengths": ["a relevant CV strength for this job — skill, experience, education, or competency"],
              "suggestions": ["a concrete improvement suggestion — content gaps, missing keywords, or CV structure/ATS-friendliness issues"]
            }
            matchScore is an integer from 0 to 100 estimating overall fit across skills, experience, education,
            competencies, and location combined.

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
        log.info("Calling Gemini model {}", model);

        String response;
        try {
            response = webClient.post()
                    .uri("/v1beta/models/" + model + ":generateContent")
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(60))
                    .block();
        } catch (RuntimeException e) {
            log.error("Gemini call failed", e);
            throw e;
        }

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
            log.error("Failed to parse Gemini response: {}", response, e);
            throw new RuntimeException("Failed to parse Gemini response: " + e.getMessage());
        }
    }
}