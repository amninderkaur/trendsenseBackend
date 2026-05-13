package capstoneBackend.ca.sheridancollege.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import capstoneBackend.ca.sheridancollege.beans.ShoppingRequest;
import capstoneBackend.ca.sheridancollege.beans.ShoppingSuggestionsResponse;
import capstoneBackend.ca.sheridancollege.beans.ShoppingSuggestionsResponse.ShoppingSuggestion;
import capstoneBackend.ca.sheridancollege.beans.UserProfile;
import capstoneBackend.ca.sheridancollege.beans.WardrobeItem;
import capstoneBackend.ca.sheridancollege.beans.repositories.UserProfileRepository;
import capstoneBackend.ca.sheridancollege.beans.repositories.WardrobeRepository;
import capstoneBackend.ca.sheridancollege.service.GeminiService.GroundedResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@AllArgsConstructor
public class ShoppingService {

    private static final List<String> STANDARD_CATEGORIES = List.of(
            "tops", "bottoms", "outerwear", "shoes", "accessories", "dresses", "activewear"
    );

    private final GeminiService geminiService;
    private final WardrobeRepository wardrobeRepository;
    private final UserProfileRepository userProfileRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ShoppingSuggestionsResponse suggest(String userId, ShoppingRequest request) {

        // Step 1: Fetch user profile
        UserProfile profile = userProfileRepository.findByUserId(userId).orElse(null);
        String season = profile != null && profile.getColourSeason() != null
                ? profile.getColourSeason() : "unknown";
        String genderAesthetic = profile != null && profile.getGenderAesthetic() != null
                ? profile.getGenderAesthetic() : "mixed";
        String modestyLevel = profile != null && profile.getModestyLevel() != null
                ? profile.getModestyLevel() : "medium";
        String culturalPrefs = profile != null && profile.getCulturalPreferences() != null
                ? String.join(", ", profile.getCulturalPreferences()) : "none";

        // Step 2: Fetch wardrobe and identify gaps
        List<WardrobeItem> wardrobe = wardrobeRepository.findByUserId(userId);
        List<String> gaps = identifyGaps(wardrobe, request.getFocusCategory());

        // Step 3: Build Gemini prompt (exact wording from spec)
        String gapsText = gaps.isEmpty() ? "general wardrobe refresh" : String.join(", ", gaps);
        String storePreference = request.isPreferOnline() ? "Focus on online stores." : "Include both online and nearby physical stores.";

        String prompt = String.format(
            "The user is located in %s. Their colour season is %s. " +
            "Based on their wardrobe they are missing: %s. " +
            "Their budget is %.2f %s. " +
            "Style preference: %s, modesty level: %s, cultural preferences: %s. " +
            "%s " +
            "Search for real products available in Canada matching these gaps and budget. " +
            "Include online and nearby physical store options. " +
            "Return ONLY a valid JSON array of suggestions with fields: " +
            "item, category, whyItFits, estimatedPrice, storeName, storeType, link, nearbyLocation. " +
            "No markdown, no extra text.",
            request.getLocation(),
            season,
            gapsText,
            request.getBudget(),
            request.getCurrency() != null ? request.getCurrency() : "CAD",
            genderAesthetic,
            modestyLevel,
            culturalPrefs,
            storePreference
        );

        // Step 4: Call Gemini with Google Search grounding
        GroundedResponse grounded = geminiService.sendTextPromptWithGrounding(prompt);
        if (grounded == null || grounded.text() == null) {
            log.error("Gemini returned null for shopping suggestions");
            return ShoppingSuggestionsResponse.builder()
                    .season(season)
                    .gapsIdentified(gaps)
                    .suggestions(List.of())
                    .build();
        }

        // Step 5: Parse JSON suggestions from Gemini text
        List<ShoppingSuggestion> suggestions = parseAndMerge(grounded.text(), grounded.groundingUrls());

        // Step 6: Calculate total estimate and budget check
        String totalEstimate = estimateTotal(suggestions);
        boolean withinBudget = checkBudget(suggestions, request.getBudget());

        return ShoppingSuggestionsResponse.builder()
                .season(season)
                .gapsIdentified(gaps)
                .totalEstimate(totalEstimate)
                .withinBudget(withinBudget)
                .suggestions(suggestions)
                .build();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private List<String> identifyGaps(List<WardrobeItem> wardrobe, String focusCategory) {
        if (focusCategory != null && !focusCategory.isBlank()) {
            return List.of(focusCategory.toLowerCase());
        }

        Set<String> owned = wardrobe.stream()
                .filter(item -> item.getTag() != null)
                .map(item -> item.getTag().toLowerCase())
                .collect(Collectors.toSet());

        return STANDARD_CATEGORIES.stream()
                .filter(cat -> owned.stream().noneMatch(tag -> tag.contains(cat)))
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private List<ShoppingSuggestion> parseAndMerge(String text, List<String> groundingUrls) {
        try {
            String cleaned = stripMarkdown(text);

            // Gemini may wrap the array in a root object — try array first, then object
            List<Map<String, Object>> raw;
            if (cleaned.trim().startsWith("[")) {
                raw = objectMapper.readValue(cleaned, new TypeReference<>() {});
            } else {
                Map<String, Object> wrapper = objectMapper.readValue(cleaned, new TypeReference<>() {});
                Object inner = wrapper.values().iterator().next();
                raw = (List<Map<String, Object>>) inner;
            }

            List<ShoppingSuggestion> result = new ArrayList<>();
            for (int i = 0; i < raw.size(); i++) {
                Map<String, Object> s = raw.get(i);

                // Use grounding URL if available, otherwise fall back to Gemini's link field
                String groundingUrl = (groundingUrls != null && i < groundingUrls.size())
                        ? groundingUrls.get(i) : null;
                String geminiLink = str(s, "link");
                String link = groundingUrl != null ? groundingUrl : geminiLink;

                result.add(ShoppingSuggestion.builder()
                        .item(str(s, "item"))
                        .category(str(s, "category"))
                        .whyItFits(str(s, "whyItFits"))
                        .estimatedPrice(str(s, "estimatedPrice"))
                        .storeName(str(s, "storeName"))
                        .storeType(str(s, "storeType"))
                        .link(link)
                        .nearbyLocation(str(s, "nearbyLocation"))
                        .build());
            }
            return result;

        } catch (Exception e) {
            log.error("Failed to parse shopping suggestions: {}", e.getMessage());
            return List.of();
        }
    }

    private String estimateTotal(List<ShoppingSuggestion> suggestions) {
        // Best-effort: sum up numeric parts of estimatedPrice strings
        double total = 0;
        for (ShoppingSuggestion s : suggestions) {
            if (s.getEstimatedPrice() != null) {
                try {
                    String numeric = s.getEstimatedPrice().replaceAll("[^0-9.]", "");
                    if (!numeric.isBlank()) total += Double.parseDouble(numeric);
                } catch (NumberFormatException ignored) {}
            }
        }
        return total > 0 ? String.format("$%.2f CAD", total) : "N/A";
    }

    private boolean checkBudget(List<ShoppingSuggestion> suggestions, double budget) {
        double total = 0;
        for (ShoppingSuggestion s : suggestions) {
            if (s.getEstimatedPrice() != null) {
                try {
                    String numeric = s.getEstimatedPrice().replaceAll("[^0-9.]", "");
                    if (!numeric.isBlank()) total += Double.parseDouble(numeric);
                } catch (NumberFormatException ignored) {}
            }
        }
        return total <= budget;
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

    private String str(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : null;
    }
}
