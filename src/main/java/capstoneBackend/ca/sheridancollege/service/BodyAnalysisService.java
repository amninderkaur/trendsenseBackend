package capstoneBackend.ca.sheridancollege.service;

import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import capstoneBackend.ca.sheridancollege.beans.BodyAnalysisResponse;
import capstoneBackend.ca.sheridancollege.beans.UserProfile;
import capstoneBackend.ca.sheridancollege.beans.repositories.UserProfileRepository;
import capstoneBackend.ca.sheridancollege.util.GeminiUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@AllArgsConstructor
public class BodyAnalysisService {

    private final GeminiService geminiService;
    private final UserProfileRepository userProfileRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public BodyAnalysisResponse analyzeBody(
            String userId,
            byte[] imageBytes,
            String mimeType,
            String height,
            String weight,
            String chest,
            String waist,
            String hips) {

        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        // Build optional measurements block
        StringBuilder measurements = new StringBuilder();
        if (isPresent(height))  measurements.append("- Height: ").append(height).append("\n");
        if (isPresent(weight))  measurements.append("- Weight: ").append(weight).append("\n");
        if (isPresent(chest))   measurements.append("- Chest: ").append(chest).append("\n");
        if (isPresent(waist))   measurements.append("- Waist: ").append(waist).append("\n");
        if (isPresent(hips))    measurements.append("- Hips: ").append(hips).append("\n");

        String measurementsSection = measurements.length() > 0
            ? "The user has also provided these measurements:\n" + measurements + "\n"
            : "";

        String prompt =
            "You are a professional personal stylist specialising in body shape analysis.\n\n" +
            "STEP 1 — Validate the photo. Check whether the image shows a full human body " +
            "from head to toe (the top of the head AND both feet must be visible) in reasonably " +
            "fitted clothing (not a very baggy outfit that hides the silhouette). " +
            "If the photo does NOT meet this requirement — e.g. it is a close-up, cropped below " +
            "the knees, only the torso, an object, or a very baggy outfit — return ONLY:\n" +
            "{\"error\": \"NOT_FULL_BODY\", \"errorReason\": \"Please upload a full body photo from head to toe in fitted clothing so we can accurately analyse your proportions.\"}\n\n" +
            "STEP 2 — If the photo IS valid, determine the body shape by analysing:\n" +
            "- Shoulder width relative to hip width\n" +
            "- Waist definition (is the waist noticeably narrower than shoulders/hips?)\n" +
            "- Overall silhouette and proportions\n" +
            measurementsSection +
            "The five body shapes are:\n" +
            "- Hourglass: shoulders and hips roughly equal width, clearly defined waist\n" +
            "- Pear (Triangle): hips noticeably wider than shoulders, smaller upper body\n" +
            "- Apple (Round): midsection is the widest point, slimmer legs\n" +
            "- Rectangle (Straight): shoulders, waist, and hips roughly the same width, little waist definition\n" +
            "- Inverted Triangle: shoulders noticeably wider than hips\n\n" +
            "Return a JSON object with exactly these fields:\n" +
            "{\n" +
            "  \"bodyShape\": \"<one of: Hourglass | Pear | Apple | Rectangle | Inverted Triangle>\",\n" +
            "  \"summary\": \"<2-3 sentences describing their shape and what that means for dressing>\",\n" +
            "  \"bestStyles\": [\"<style 1>\", \"<style 2>\", \"<style 3>\", \"<style 4>\", \"<style 5>\"],\n" +
            "  \"stylesToAvoid\": [\"<style 1>\", \"<style 2>\", \"<style 3>\"],\n" +
            "  \"tips\": [\"<tip 1>\", \"<tip 2>\", \"<tip 3>\"]\n" +
            "}\n" +
            "Respond in valid JSON only. No extra text, no markdown, no code blocks.";

        String geminiResponse = geminiService.sendImagePrompt(base64Image, mimeType, prompt);
        if (geminiResponse == null) {
            log.error("Gemini returned null for body analysis");
            return null;
        }

        BodyAnalysisResponse result;
        try {
            String cleaned = GeminiUtils.stripMarkdownCodeBlock(geminiResponse);
            Map<String, Object> raw = objectMapper.readValue(cleaned, new TypeReference<>() {});

            // Check if Gemini flagged a validation error
            if (raw.containsKey("error")) {
                log.warn("Body analysis rejected photo for user {}: {}", userId, raw.get("errorReason"));
                return BodyAnalysisResponse.builder()
                        .error((String) raw.get("error"))
                        .errorReason((String) raw.get("errorReason"))
                        .build();
            }

            result = objectMapper.convertValue(raw, BodyAnalysisResponse.class);
        } catch (Exception e) {
            log.error("Failed to parse body analysis response: {}", e.getMessage());
            return null;
        }

        // Save to user profile
        try {
            UserProfile profile = userProfileRepository.findByUserId(userId).orElse(new UserProfile());
            profile.setUserId(userId);
            profile.setBodyShape(result.getBodyShape());
            profile.setBodyAnalysisSummary(result.getSummary());
            profile.setBodyBestStyles(result.getBestStyles());
            profile.setBodyStylesToAvoid(result.getStylesToAvoid());
            userProfileRepository.save(profile);
            log.info("Saved body shape '{}' for user {}", result.getBodyShape(), userId);
        } catch (Exception e) {
            log.error("Failed to save body analysis for user {}: {}", userId, e.getMessage());
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private List<String> toStringList(Object val) {
        if (val instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        return List.of();
    }

    private boolean isPresent(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
