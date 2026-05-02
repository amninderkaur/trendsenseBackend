package capstoneBackend.ca.sheridancollege.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import capstoneBackend.ca.sheridancollege.beans.ClothingItem;
import lombok.extern.slf4j.Slf4j;

/**
 * Wraps two Gemini API calls:
 *  1. gemini-2.0-flash:generateContent  – detects clothing items in an image
 *  2. imagen-3.0-generate-002:predict   – generates a clean product image per item
 */
@Slf4j
@Service
public class GeminiService {

    private static final String DETECT_PROMPT =
            "You are a fashion AI. Analyze this photo and list every clothing item you can see. " +
            "For each item return a JSON array. Each object must have: " +
            "type (e.g. jacket, jeans, dress, sneakers), " +
            "color (specific color name), " +
            "style (casual / formal / streetwear / athletic / smart-casual), " +
            "occasion (array: casual / work / date night / formal / gym / beach), " +
            "description (one sentence describing the item for image generation). " +
            "Return ONLY the JSON array, no other text.";

    private static final String GENERATE_PROMPT_TEMPLATE =
            "Clean product photography of a %s. " +
            "White background, studio lighting, no model, no mannequin. " +
            "Sharp focus, high quality fashion retail style.";

    private final WebClient geminiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gemini.api.key}")
    private String apiKey;

    public GeminiService(WebClient geminiClient) {
        this.geminiClient = geminiClient;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Detects all clothing items visible in the supplied image.
     *
     * @param base64Image base64-encoded image bytes (no "data:..." prefix)
     * @param mimeType    e.g. "image/jpeg"
     * @return list of detected items; empty list if none found or on error
     */
    public List<DetectedItem> detectItems(String base64Image, String mimeType) {
        Map<String, Object> requestBody = buildDetectRequest(base64Image, mimeType);

        try {
            Map<?, ?> response = geminiClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/models/gemini-2.0-flash:generateContent")
                            .queryParam("key", apiKey)
                            .build())
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return parseDetectResponse(response);

        } catch (WebClientResponseException e) {
            log.error("Gemini detect call failed: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            return List.of();
        } catch (Exception e) {
            log.error("Unexpected error during Gemini detect call", e);
            return List.of();
        }
    }

    /**
     * Generates a clean, white-background product image for one clothing item.
     *
     * @param description one-sentence description from the detect step
     * @return base64-encoded PNG, or null if generation failed
     */
    public String generateItemImage(String description) {
        String prompt = String.format(GENERATE_PROMPT_TEMPLATE, description);
        Map<String, Object> requestBody = buildGenerateRequest(prompt);

        try {
            Map<?, ?> response = geminiClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/models/gemini-2.0-flash:generateContent")
                            .queryParam("key", apiKey)
                            .build())
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return parseGenerateResponse(response);

        } catch (WebClientResponseException e) {
            log.error("Gemini generate call failed: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            log.error("Unexpected error during Gemini generate call", e);
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Request builders
    // -------------------------------------------------------------------------

    private Map<String, Object> buildDetectRequest(String base64Image, String mimeType) {
        return Map.of(
            "contents", List.of(
                Map.of("parts", List.of(
                    Map.of("inlineData", Map.of(
                        "mimeType", mimeType,
                        "data", base64Image
                    )),
                    Map.of("text", DETECT_PROMPT)
                ))
            )
        );
    }

    private Map<String, Object> buildGenerateRequest(String prompt) {
        return Map.of(
            "contents", List.of(
                Map.of("parts", List.of(Map.of("text", prompt)))
            ),
            "generationConfig", Map.of(
                "responseModalities", List.of("IMAGE")
            )
        );
    }

    // -------------------------------------------------------------------------
    // Response parsers
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private List<DetectedItem> parseDetectResponse(Map<?, ?> response) {
        if (response == null) return List.of();

        try {
            List<?> candidates = (List<?>) response.get("candidates");
            if (candidates == null || candidates.isEmpty()) return List.of();

            Map<?, ?> firstCandidate = (Map<?, ?>) candidates.get(0);
            Map<?, ?> content = (Map<?, ?>) firstCandidate.get("content");
            List<?> parts = (List<?>) content.get("parts");
            if (parts == null || parts.isEmpty()) return List.of();

            String text = (String) ((Map<?, ?>) parts.get(0)).get("text");
            if (text == null || text.isBlank()) return List.of();

            // Gemini sometimes wraps output in ```json ... ``` — strip it
            text = stripMarkdownCodeBlock(text);

            List<Map<String, Object>> items = objectMapper.readValue(
                    text, new TypeReference<>() {});

            List<DetectedItem> result = new ArrayList<>();
            for (Map<String, Object> item : items) {
                DetectedItem d = new DetectedItem();
                d.setType(getString(item, "type"));
                d.setColor(getString(item, "color"));
                d.setStyle(getString(item, "style"));
                d.setDescription(getString(item, "description"));

                Object occ = item.get("occasion");
                if (occ instanceof List<?> occList) {
                    d.setOccasion(occList.stream()
                            .map(Object::toString)
                            .toList());
                } else {
                    d.setOccasion(List.of());
                }
                result.add(d);
            }
            return result;

        } catch (Exception e) {
            log.error("Failed to parse Gemini detect response: {}", e.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private String parseGenerateResponse(Map<?, ?> response) {
        if (response == null) return null;

        try {
            List<?> candidates = (List<?>) response.get("candidates");
            if (candidates == null || candidates.isEmpty()) return null;

            Map<?, ?> content = (Map<?, ?>) ((Map<?, ?>) candidates.get(0)).get("content");
            if (content == null) return null;

            List<?> parts = (List<?>) content.get("parts");
            if (parts == null || parts.isEmpty()) return null;

            // Find the first part that contains an inlineData image
            for (Object part : parts) {
                Map<?, ?> partMap = (Map<?, ?>) part;
                Map<?, ?> inlineData = (Map<?, ?>) partMap.get("inlineData");
                if (inlineData != null) {
                    return (String) inlineData.get("data");
                }
            }
            return null;

        } catch (Exception e) {
            log.error("Failed to parse Gemini generate response: {}", e.getMessage());
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String stripMarkdownCodeBlock(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline != -1) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.lastIndexOf("```")).trim();
            }
        }
        return trimmed;
    }

    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : "";
    }

    // -------------------------------------------------------------------------
    // Inner DTO
    // -------------------------------------------------------------------------

    @lombok.Data
    public static class DetectedItem {
        private String type;
        private String color;
        private String style;
        private List<String> occasion;
        private String description;

        public ClothingItem.Tags toTags() {
            return new ClothingItem.Tags(type, color, style, occasion);
        }
    }
}
