package capstoneBackend.ca.sheridancollege.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import capstoneBackend.ca.sheridancollege.beans.ClothingItem;
import capstoneBackend.ca.sheridancollege.beans.ShoppingRequest;
import capstoneBackend.ca.sheridancollege.beans.ShoppingSuggestionsResponse;
import capstoneBackend.ca.sheridancollege.beans.ShoppingSuggestionsResponse.ShoppingSuggestion;
import capstoneBackend.ca.sheridancollege.beans.UserProfile;
import capstoneBackend.ca.sheridancollege.beans.repositories.ClothingRepository;
import capstoneBackend.ca.sheridancollege.beans.repositories.UserProfileRepository;
import capstoneBackend.ca.sheridancollege.service.GeminiService.GroundedResponse;
import capstoneBackend.ca.sheridancollege.util.GeminiUtils;
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
    private final ClothingRepository clothingRepository;
    private final UserProfileRepository userProfileRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ShoppingSuggestionsResponse suggest(String userId, ShoppingRequest request) {

        // Step 1: Fetch user profile and extract style context
        UserProfile profile = userProfileRepository.findByUserId(userId).orElse(null);
        UserContext ctx = buildUserContext(profile);

        // Step 2: Fetch wardrobe and identify gaps
        List<ClothingItem> wardrobe = clothingRepository.findByUserId(userId);
        List<String> gaps = identifyGaps(wardrobe, request.getFocusCategory());

        // Step 3: Build Gemini prompt
        String gapsText = gaps.isEmpty() ? "general wardrobe refresh" : String.join(", ", gaps);
        String storePreference = request.isPreferOnline() ? "Focus on online stores." : "Include both online and nearby physical stores.";

        String prompt = String.format(
            "You are a personal fashion shopping assistant. The user's profile is as follows:\n" +
            "- Location: %s\n" +
            "- Age group: %s, Gender: %s\n" +
            "- Colour season: %s\n" +
            "- Style preferences: %s\n" +
            "- Favourite colours: %s; Colours to avoid: %s\n" +
            "- Preferred fit: %s; Preferred fabrics: %s\n" +
            "- Dresses for: %s; Climate: %s\n" +
            "- Budget per item: %s; Overall budget for this trip: %.2f %s\n" +
            "- Shopping priorities: %s\n" +
            "- Favourite brands: %s; Brands to avoid: %s\n" +
            "- Gender aesthetic: %s; Modesty level: %s; Cultural preferences: %s\n\n" +
            "Based on their wardrobe they are missing: %s.\n" +
            "%s\n" +
            "Search for real products available in Canada that match the user's style, fit, fabric, colour and brand preferences. " +
            "Respect their colours-to-avoid and brands-to-avoid lists. " +
            "Keep suggestions within their budget per item where possible. " +
            "Return ONLY a valid JSON array of suggestions with fields: " +
            "item, category, whyItFits, estimatedPrice, storeName, storeType, link, nearbyLocation. " +
            "No markdown, no extra text.",
            request.getLocation(),
            ctx.ageGroup(), ctx.gender(),
            ctx.season(),
            ctx.styles(),
            ctx.favColors(), ctx.avoidColors(),
            ctx.fit(), ctx.fabrics(),
            ctx.dressFor(), ctx.climate(),
            ctx.budgetPerItem(), request.getBudget(), request.getCurrency() != null ? request.getCurrency() : "CAD",
            ctx.priorities(),
            ctx.favBrands(), ctx.avoidBrands(),
            ctx.genderAesthetic(), ctx.modesty(), ctx.culturalPrefs(),
            gapsText,
            storePreference
        );

        // Step 4: Call Gemini with Google Search grounding
        GroundedResponse grounded = geminiService.sendTextPromptWithGrounding(prompt);
        if (grounded == null || grounded.text() == null) {
            log.error("Gemini returned null for shopping suggestions");
            return ShoppingSuggestionsResponse.builder()
                    .season(ctx.season())
                    .gapsIdentified(gaps)
                    .suggestions(List.of())
                    .build();
        }

        // Step 5: Parse JSON suggestions from Gemini text
        List<ShoppingSuggestion> suggestions = parseAndMerge(grounded.text(), grounded.groundingUrls());

        // Step 6: Calculate total estimate and budget check
        double total = parseTotalPrice(suggestions);
        String totalEstimate = total > 0 ? String.format("$%.2f CAD", total) : "N/A";
        boolean withinBudget = total <= request.getBudget();

        return ShoppingSuggestionsResponse.builder()
                .season(ctx.season())
                .gapsIdentified(gaps)
                .totalEstimate(totalEstimate)
                .withinBudget(withinBudget)
                .suggestions(suggestions)
                .build();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Holds all style-related user preference strings extracted from the profile. */
    private record UserContext(
        String season, String genderAesthetic, String modesty, String culturalPrefs,
        String styles, String favColors, String avoidColors, String fit, String fabrics,
        String dressFor, String climate, String budgetPerItem,
        String favBrands, String avoidBrands, String priorities,
        String gender, String ageGroup
    ) {}

    private UserContext buildUserContext(UserProfile profile) {
        return new UserContext(
            val(profile != null ? profile.getColourSeason()         : null, "unknown"),
            val(profile != null ? profile.getGenderAesthetic()      : null, "mixed"),
            val(profile != null ? profile.getModestyLevel()         : null, "medium"),
            join(profile != null ? profile.getCulturalPreferences() : null, "none"),
            join(profile != null ? profile.getStyles()              : null, "any"),
            join(profile != null ? profile.getFavoriteColors()      : null, "any"),
            join(profile != null ? profile.getColorsToAvoid()       : null, "none"),
            join(profile != null ? profile.getPreferredFit()        : null, "any"),
            join(profile != null ? profile.getPreferredFabrics()    : null, "any"),
            join(profile != null ? profile.getDressFor()            : null, "general"),
            val(profile != null ? profile.getClimate()              : null, "mixed"),
            val(profile != null ? profile.getBudgetPerItem()        : null, "flexible"),
            join(profile != null ? profile.getFavoriteBrands()      : null, "no preference"),
            join(profile != null ? profile.getBrandsToAvoid()       : null, "none"),
            join(profile != null ? profile.getShoppingPriorities()  : null, "price, quality"),
            val(profile != null ? profile.getGender()               : null, "unspecified"),
            val(profile != null ? profile.getAgeGroup()             : null, "adult")
        );
    }

    private List<String> identifyGaps(List<ClothingItem> wardrobe, String focusCategory) {
        if (focusCategory != null && !focusCategory.isBlank()) {
            return List.of(focusCategory.toLowerCase());
        }

        Set<String> owned = wardrobe.stream()
                .filter(item -> item.getTags() != null && item.getTags().getType() != null)
                .map(item -> item.getTags().getType().toLowerCase())
                .collect(Collectors.toSet());

        return STANDARD_CATEGORIES.stream()
                .filter(cat -> owned.stream().noneMatch(tag -> tag.contains(cat)))
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private List<ShoppingSuggestion> parseAndMerge(String text, List<String> groundingUrls) {
        try {
            String cleaned = GeminiUtils.stripMarkdownCodeBlock(text);

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

    /** Sums the numeric portions of each suggestion's estimatedPrice field. */
    private double parseTotalPrice(List<ShoppingSuggestion> suggestions) {
        double total = 0;
        for (ShoppingSuggestion s : suggestions) {
            if (s.getEstimatedPrice() != null) {
                try {
                    String numeric = s.getEstimatedPrice().replaceAll("[^0-9.]", "");
                    if (!numeric.isBlank()) total += Double.parseDouble(numeric);
                } catch (NumberFormatException ignored) {}
            }
        }
        return total;
    }

    private String str(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : null;
    }

    private String val(String value, String fallback) {
        return (value != null && !value.isBlank()) ? value : fallback;
    }

    private String join(List<String> list, String fallback) {
        return (list != null && !list.isEmpty()) ? String.join(", ", list) : fallback;
    }
}
