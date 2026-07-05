package za.ac.cput.jobassistantapi.service.impl;

import org.springframework.stereotype.Service;
import za.ac.cput.jobassistantapi.dto.response.SkillExtractionResult;
import za.ac.cput.jobassistantapi.service.SkillExtractionService;
import za.ac.cput.jobassistantapi.util.SkillDictionary;

import java.util.regex.Pattern;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;



@Service
public class SkillExtractionServiceImpl implements SkillExtractionService {

    private boolean matchesSkill(String text, String skill) {
        String escapedSkill = Pattern.quote(skill);
        return text.matches(".*\\b" + escapedSkill + "\\b.*");
    }

    @Override
    public SkillExtractionResult extract(String text) {

        if (text == null || text.isBlank()) {
            return new SkillExtractionResult(List.of());
        }

        String normalizedText = normalize(text);

        List<String> foundSkills = new ArrayList<>();

        for (String skill : SkillDictionary.SKILLS) {

            String normalizedSkill = skill.toLowerCase();

            if (matchesSkill(normalizedText, normalizedSkill)) {
                foundSkills.add(skill);
            }
        }

        return new SkillExtractionResult(foundSkills);
    }

}