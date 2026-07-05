package za.ac.cput.jobassistantapi.service.impl;

import org.springframework.stereotype.Service;
import za.ac.cput.jobassistantapi.service.TextCleaningService;

@Service
public class TextCleaningServiceImpl implements TextCleaningService {

    @Override
    public String clean(String rawText) {

        if (rawText == null) return "";

        return rawText
                .replaceAll("\\r\\n|\\r|\\n", " ")

                .replaceAll("\\s+", " ")

                .replaceAll("[^\\x20-\\x7E]", " ")

                .trim();
    }

    private String normalize(String text) {
        return text.toLowerCase()
                .replaceAll("[^a-z0-9\\s+]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}