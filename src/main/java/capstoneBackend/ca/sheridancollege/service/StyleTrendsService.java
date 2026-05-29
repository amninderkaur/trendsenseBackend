package capstoneBackend.ca.sheridancollege.service;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import capstoneBackend.ca.sheridancollege.beans.ClothingItem;
import capstoneBackend.ca.sheridancollege.beans.StyleTrendResult;
import capstoneBackend.ca.sheridancollege.beans.StyleTrendResult.TrendItem;
import capstoneBackend.ca.sheridancollege.beans.StyleTrendResult.WardrobeMatch;
import capstoneBackend.ca.sheridancollege.beans.repositories.ClothingRepository;
import capstoneBackend.ca.sheridancollege.service.GeminiService.GroundedResponse;
import capstoneBackend.ca.sheridancollege.util.GeminiUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@AllArgsConstructor
public class StyleTrendsService {

    /** Maximum wardrobe items sent to Gemini for matching (~150 tokens each, caps at ~3000 tokens). */
    private static final int MAX_WARDROBE_ITEMS = 20;

    private final GeminiService geminiService;
    private final ClothingRepository clothingRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public StyleTrendResult getTrendsWithWardrobeMatches(String userId) {
        LocalDateTime now = LocalDateTime.now();
        String currentSeason = resolveSeason(now.getMonth());
        String currentYear = String.valueOf(now.getYear());

        // ── Step 1: Fetch current trends via Gemini + Google Search grounding ──
        String trendsPrompt = String.format(
            "Search for current fashion trends for %s %s right now.\n" +
            "Focus on wearable everyday trends, not runway only.\n\n" +
            "Return ONLY this JSON, nothing else:\n" +
            "{\n" +
            "  \"season\": \"%s %s\",\n" +
            "  \"trends\": [\n" +
            "    {\n" +
            "      \"trendName\": \"Quiet Luxury\",\n" +
            "      \"description\": \"...\",\n" +
            "      \"keyPieces\": [\"cashmere knit\", \"wide leg trousers\"],\n" +
            "      \"colors\": [\"cream\", \"camel\", \"navy\"],\n" +
            "      \"wearItHow\": \"...\"\n" +
            "    }\n" +
            "  ]\n" +
            "}\n" +
            "Return 5 trends maximum.",
            currentSeason, currentYear,
            currentSeason, currentYear
        );

        GroundedResponse grounded = geminiService.sendTextPromptWithGrounding(trendsPrompt);
        if (grounded == null || grounded.text() == null) {
            log.error("Gemini returned null for style trends fetch");
            return StyleTrendResult.builder()
                    .season(currentSeason)
                    .year(currentYear)
                    .trends(List.of())
                    .wardrobeMatches(List.of())
                    .summary("Could not fetch trends at this time.")
                    .fetchedAt(now)
                    .build();
        }

        // ── Step 2: Parse trends JSON ──
        List<TrendItem> trends = parseTrends(grounded.text());
        if (trends.isEmpty()) {
            log.warn("No trends parsed from Gemini response");
            return StyleTrendResult.builder()
                    .season(currentSeason)
                    .year(currentYear)
                    .trends(List.of())
                    .wardrobeMatches(List.of())
                    .summary("No trend data available right now.")
                    .fetchedAt(now)
                    .build();
        }

        // ── Step 3: Load user's wardrobe ──
        List<ClothingItem> wardrobe = clothingRepository.findByUserId(userId);

        // ── Step 4 & 5: Match wardrobe items to trends (cap at MAX_WARDROBE_ITEMS) ──
        String trendContext = buildTrendContext(trends);
        List<ClothingItem> itemsToCheck = wardrobe.size() > MAX_WARDROBE_ITEMS
                ? wardrobe.subList(0, MAX_WARDROBE_ITEMS)
                : wardrobe;

        List<WardrobeMatch> wardrobeMatches = new ArrayList<>();
        for (ClothingItem item : itemsToCheck) {
            if (item.getTags() == null) continue;

            WardrobeMatch match = matchItemToTrends(item, trendContext);
            if (match != null && !match.getMatchingTrends().isEmpty()) {
                wardrobeMatches.add(match);
            }
        }

        // ── Step 6: Build and return result ──
        String summary = wardrobeMatches.isEmpty()
                ? "None of your current wardrobe items match " + currentSeason + " " + currentYear + " trends yet."
                : "You already own " + wardrobeMatches.size() + " item" + (wardrobeMatches.size() == 1 ? "" : "s") +
                  " that match" + (wardrobeMatches.size() == 1 ? "es" : "") +
                  " current " + currentSeason + " trends";

        return StyleTrendResult.builder()
                .season(currentSeason)
                .year(currentYear)
                .trends(trends)
                .wardrobeMatches(wardrobeMatches)
                .summary(summary)
                .fetchedAt(now)
                .build();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Returns the fashion season name for a given calendar month. */
    private String resolveSeason(Month month) {
        return switch (month) {
            case MARCH, APRIL, MAY -> "Spring";
            case JUNE, JULY, AUGUST -> "Summer";
            case SEPTEMBER, OCTOBER, NOVEMBER -> "Fall";
            case DECEMBER, JANUARY, FEBRUARY -> "Winter";
        };
    }

    /** Builds a compact plain-text summary of current trends to include in wardrobe-match prompts. */
    private String buildTrendContext(List<TrendItem> trends) {
        return trends.stream()
                .map(t -> t.getTrendName() + ": key pieces — " + String.join(", ", t.getKeyPieces()))
                .collect(Collectors.joining("\n"));
    }

    /**
     * Asks Gemini (no grounding — pure reasoning) which trends a single wardrobe item fits.
     * Returns null on parse failure; returns a WardrobeMatch with empty matchingTrends if nothing fits.
     */
    @SuppressWarnings("unchecked")
    private WardrobeMatch matchItemToTrends(ClothingItem item, String trendContext) {
        ClothingItem.Tags tags = item.getTags();
        String prompt = String.format(
            "Current trends:\n%s\n\n" +
            "Wardrobe item: type=%s, color=%s, style=%s\n\n" +
            "Which of these trends does this item fit, if any?\n" +
            "Reply ONLY with JSON:\n" +
            "{ \"matchingTrends\": [\"Quiet Luxury\"], \"stylingTip\": \"Pair with...\" }\n" +
            "If it fits no trends reply: { \"matchingTrends\": [], \"stylingTip\": \"\" }",
            trendContext,
            val(tags.getType()),
            val(tags.getColor()),
            val(tags.getStyle())
        );

        String response = geminiService.sendTextPrompt(prompt);
        if (response == null) {
            log.warn("Gemini returned null for wardrobe match on item {}", item.getId());
            return null;
        }

        try {
            String cleaned = GeminiUtils.stripMarkdownCodeBlock(response);
            Map<String, Object> parsed = objectMapper.readValue(cleaned, new TypeReference<>() {});

            List<String> matchingTrends = new ArrayList<>();
            Object rawTrends = parsed.get("matchingTrends");
            if (rawTrends instanceof List<?> list) {
                list.forEach(t -> matchingTrends.add(t.toString()));
            }

            String stylingTip = parsed.get("stylingTip") != null ? parsed.get("stylingTip").toString() : "";

            return WardrobeMatch.builder()
                    .itemId(item.getId())
                    .itemType(tags.getType())
                    .color(tags.getColor())
                    .imageBase64(item.getGeneratedImageBase64())
                    .matchingTrends(matchingTrends)
                    .stylingTip(stylingTip)
                    .build();

        } catch (Exception e) {
            log.warn("Failed to parse wardrobe match for item {}: {}", item.getId(), e.getMessage());
            return null;
        }
    }

    /** Parses the trends JSON block returned by Gemini in Step 1. */
    @SuppressWarnings("unchecked")
    private List<TrendItem> parseTrends(String text) {
        try {
            String cleaned = GeminiUtils.stripMarkdownCodeBlock(text);

            Map<String, Object> wrapper = objectMapper.readValue(cleaned, new TypeReference<>() {});
            Object rawTrends = wrapper.get("trends");
            if (!(rawTrends instanceof List<?> rawList)) {
                log.error("Trends JSON missing 'trends' array");
                return List.of();
            }

            List<TrendItem> result = new ArrayList<>();
            for (Object entry : rawList) {
                Map<String, Object> t = (Map<String, Object>) entry;

                List<String> keyPieces = toStringList(t.get("keyPieces"));
                List<String> colors = toStringList(t.get("colors"));

                result.add(TrendItem.builder()
                        .trendName(str(t, "trendName"))
                        .description(str(t, "description"))
                        .keyPieces(keyPieces)
                        .colors(colors)
                        .wearItHow(str(t, "wearItHow"))
                        .build());
            }
            return result;

        } catch (Exception e) {
            log.error("Failed to parse trends from Gemini response: {}", e.getMessage());
            return List.of();
        }
    }

    private List<String> toStringList(Object raw) {
        if (!(raw instanceof List<?> list)) return List.of();
        return list.stream().map(Object::toString).collect(Collectors.toList());
    }

    private String str(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : "";
    }

    private String val(String value) {
        return value != null ? value : "unknown";
    }
}
