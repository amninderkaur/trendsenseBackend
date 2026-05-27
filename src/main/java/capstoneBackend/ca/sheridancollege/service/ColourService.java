package capstoneBackend.ca.sheridancollege.service;

import java.util.Base64;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import capstoneBackend.ca.sheridancollege.beans.ColourAnalysisResponse;
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
     * Analyses a photo of the person combined with their self-reported colouring
     * answers to determine their 12-season colour type and recommended palette.
     * Saves results to their profile.
     */
    public ColourAnalysisResponse analyzeColourFromImage(
            String userId,
            byte[] imageBytes,
            String mimeType,
            String naturalHair,
            String currentHair,
            String eyeColor,
            String jewelry,
            String veins,
            String sunReaction) {

        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        StringBuilder userInfo = new StringBuilder();
        if (isPresent(naturalHair))   userInfo.append("- Natural hair colour: ").append(naturalHair).append("\n");
        if (isPresent(currentHair))   userInfo.append("- Current hair colour: ").append(currentHair).append("\n");
        if (isPresent(eyeColor))      userInfo.append("- Eye colour: ").append(eyeColor).append("\n");
        if (isPresent(jewelry))       userInfo.append("- Preferred jewelry: ").append(jewelry).append("\n");
        if (isPresent(veins))         userInfo.append("- Wrist vein colour: ").append(veins).append("\n");
        if (isPresent(sunReaction))   userInfo.append("- Reaction to sun: ").append(sunReaction).append("\n");

        String userSection = userInfo.length() > 0
            ? "The user has also provided these self-reported details:\n" + userInfo + "\n"
            : "";

        String prompt =
            "You are a professional personal colour analyst trained in the 12-season colour system.\n" +
            "Analyse the photo of the person and use any self-reported details provided below " +
            "to determine their exact colour season.\n\n" +
            userSection +
            "The 12 possible seasons are:\n" +
            "Light Spring, True Spring, Bright Spring, " +
            "Light Summer, True Summer, Soft Summer, " +
            "Soft Autumn, True Autumn, Deep Autumn, " +
            "Deep Winter, True Winter, Bright Winter\n\n" +
            "Return a JSON object with exactly these fields:\n" +
            "{\n" +
            "  \"season\": \"<one of the 12 seasons above, exact casing>\",\n" +
            "  \"undertone\": \"<Warm|Cool|Neutral>\",\n" +
            "  \"contrast\": \"<Low|Medium|High>\",\n" +
            "  \"bestJewelry\": \"<Gold|Silver|Both>\",\n" +
            "  \"summary\": \"<2-3 sentences describing the person's colouring and why this season suits them>\",\n" +
            "  \"recommendedColors\": [\"#hex1\", \"#hex2\", \"#hex3\", \"#hex4\", \"#hex5\", \"#hex6\", \"#hex7\", \"#hex8\", \"#hex9\"]\n" +
            "}\n" +
            "Respond in valid JSON only. No extra text, no markdown, no code blocks.";

        String geminiResponse = geminiService.sendImagePrompt(base64Image, mimeType, prompt);
        if (geminiResponse == null) {
            log.error("Gemini returned null for colour analysis");
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
            profile.setColourUndertone(result.getUndertone());
            profile.setColourContrast(result.getContrast());
            profile.setBestJewelry(result.getBestJewelry());
            profile.setRecommendedColors(result.getRecommendedColors());
            profile.setColourSummary(result.getSummary());
            userProfileRepository.save(profile);
            log.info("Saved colour season '{}' for user {}", result.getSeason(), userId);
        } catch (Exception e) {
            log.error("Failed to save colour analysis for user {}: {}", userId, e.getMessage());
        }

        return result;
    }

    private boolean isPresent(String value) {
        return value != null && !value.trim().isEmpty();
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
