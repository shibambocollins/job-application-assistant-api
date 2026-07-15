package za.ac.cput.jobassistantapi.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

@Component
public class MuseApiClient {

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://www.themuse.com")
            .build();

    public List<JsonNode> fetchJobs(String category, String level) {

        String response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/public/jobs")
                        .queryParam("category", category)
                        .queryParam("level", level)
                        .queryParam("page", 0)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();

        List<JsonNode> jobs = new ArrayList<>();
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response);
            root.path("results").forEach(jobs::add);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Muse API response: " + e.getMessage());
        }

        return jobs;
    }
}