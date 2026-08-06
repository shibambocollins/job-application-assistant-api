package za.ac.cput.jobassistantapi.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
public class MuseApiClient {

    private static final Logger log = LoggerFactory.getLogger(MuseApiClient.class);
    private static final int MAX_PAGES = 3;

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://www.themuse.com")
            .build();

    public List<JsonNode> fetchJobs(List<String> categories, String level) {

        log.info("Fetching Muse jobs for categories={} level={}", categories, level);

        List<JsonNode> jobs = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        int pageCount = 1;

        for (int page = 0; page < pageCount && page < MAX_PAGES; page++) {

            String response = fetchPage(categories, level, page);

            try {
                JsonNode root = mapper.readTree(response);
                root.path("results").forEach(jobs::add);

                if (page == 0) {
                    pageCount = root.path("page_count").asInt(1);
                }
            } catch (Exception e) {
                log.error("Failed to parse Muse API response", e);
                throw new RuntimeException("Failed to parse Muse API response: " + e.getMessage());
            }
        }

        log.info("Muse discovery returned {} jobs across {} page(s)", jobs.size(), Math.min(pageCount, MAX_PAGES));

        return jobs;
    }

    private String fetchPage(List<String> categories, String level, int page) {
        return webClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/api/public/jobs")
                            .queryParam("level", level)
                            .queryParam("page", page);
                    categories.forEach(category -> uriBuilder.queryParam("category", category));
                    return uriBuilder.build();
                })
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(30))
                .block();
    }
}
