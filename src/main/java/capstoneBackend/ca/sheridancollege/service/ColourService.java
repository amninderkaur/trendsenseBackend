package capstoneBackend.ca.sheridancollege.service;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import capstoneBackend.ca.sheridancollege.beans.ColourAnalysisResponse;
import capstoneBackend.ca.sheridancollege.beans.ColourAnalysisResponse.ColourPalette;
import capstoneBackend.ca.sheridancollege.beans.UserProfile;
import capstoneBackend.ca.sheridancollege.beans.repositories.UserProfileRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@AllArgsConstructor
public class ColourService {

    private final GeminiService geminiService;
    private final UserProfileRepository userProfileRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Analyses a photo of the person to detect their features, then determines
     * their colour season and recommended palette. Saves results to their profile.
     */
    public ColourAnalysisResponse analyzeColourFromImage(String userId, byte[] imageBytes, String mimeType) {

        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        String prompt =
            "You are a professional personal colour analyst.\n" +
            "Look at this photo of a person. Analyse their visible physical features: " +
            "skin tone, eye colour, and hair colour.\n" +
            "Based on these features, determine their colour season (Spring, Summer, Autumn, or Winter) " +
            "and recommend 3 specific hex colour codes per clothing category that will complement them.\n\n" +
            "Return a JSON object with exactly these fields:\n" +
            "{\n" +
            "  \"season\": \"<Spring|Summer|Autumn|Winter>\",\n" +
            "  \"description\": \"<1-2 sentences explaining the detected features and why these colours suit the user>\",\n" +
            "  \"palette\": {\n" +
            "    \"tops\": [\"#hex1\", \"#hex2\", \"#hex3\"],\n" +
            "    \"bottoms\": [\"#hex1\", \"#hex2\", \"#hex3\"],\n" +
            "    \"outerwear\": [\"#hex1\", \"#hex2\", \"#hex3\"],\n" +
            "    \"shoes\": [\"#hex1\", \"#hex2\", \"#hex3\"]\n" +
            "  }\n" +
            "}\n" +
            "Respond in valid JSON only. No extra text, no markdown, no code blocks.";

        String geminiResponse = geminiService.sendImagePrompt(base64Image, mimeType, prompt);
        if (geminiResponse == null) {
            log.error("Gemini returned null for image-based colour analysis");
            return null;
        }

        ColourAnalysisResponse result;
        try {
            result = objectMapper.readValue(stripMarkdown(geminiResponse), ColourAnalysisResponse.class);
        } catch (Exception e) {
            log.error("Failed to parse colour analysis response: {}", e.getMessage());
            return null;
        }

        try {
            UserProfile profile = userProfileRepository.findByUserId(userId).orElse(new UserProfile());
            profile.setUserId(userId);
            profile.setColourSeason(result.getSeason());
            if (result.getPalette() != null) {
                Map<String, List<String>> paletteMap = new HashMap<>();
                ColourPalette p = result.getPalette();
                if (p.getTops() != null) paletteMap.put("tops", p.getTops());
                if (p.getBottoms() != null) paletteMap.put("bottoms", p.getBottoms());
                if (p.getOuterwear() != null) paletteMap.put("outerwear", p.getOuterwear());
                if (p.getShoes() != null) paletteMap.put("shoes", p.getShoes());
                profile.setColourPalette(paletteMap);
            }
            userProfileRepository.save(profile);
            log.info("Saved colour season '{}' for user {}", result.getSeason(), userId);
        } catch (Exception e) {
            log.error("Failed to save colour analysis to profile for user {}: {}", userId, e.getMessage());
        }

        return result;
    }

    private String stripMarkdown(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline != -1) trimmed = trimmed.substring(firstNewline + 1);
            if (trimmed.endsWith("```")) trimmed = trimmed.substring(0, trimmed.lastIndexOf("```")).trim();
        }
        return trimmed;
    }
}
