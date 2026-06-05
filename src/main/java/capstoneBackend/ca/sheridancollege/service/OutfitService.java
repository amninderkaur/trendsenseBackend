package capstoneBackend.ca.sheridancollege.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import capstoneBackend.ca.sheridancollege.beans.ClothingItem;
import capstoneBackend.ca.sheridancollege.beans.OutfitSuggestionResponse;
import capstoneBackend.ca.sheridancollege.beans.UserProfile;
import capstoneBackend.ca.sheridancollege.beans.UserTasteProfile;
import capstoneBackend.ca.sheridancollege.beans.repositories.ClothingRepository;
import capstoneBackend.ca.sheridancollege.beans.repositories.UserProfileRepository;
import capstoneBackend.ca.sheridancollege.beans.repositories.UserTasteProfileRepository;
import capstoneBackend.ca.sheridancollege.util.GeminiUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@AllArgsConstructor
public class OutfitService {

    private final ClothingRepository clothingRepository;
    private final GeminiService geminiService;
    private final WeatherService weatherService;
    private final UserProfileRepository userProfileRepository;
    private final UserTasteProfileRepository userTasteProfileRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public OutfitSuggestionResponse suggestOutfit(String userId, String occasion, String city) {

        // Step 1: Get weather
        WeatherService.WeatherInfo weather = weatherService.getWeather(city);
        if (weather == null) {
            return OutfitSuggestionResponse.builder()
                    .reasoning("Could not fetch weather for city: " + city + ". Please check the city name and try again.")
                    .build();
        }
        String weatherSummary = String.format("%.1f°C, %s in %s", weather.temp(), weather.description(), weather.city());
        log.info("Weather for {}: {}", city, weatherSummary);

        // Step 2: Get wardrobe
        List<ClothingItem> wardrobe = clothingRepository.findByUserId(userId);
        if (wardrobe.isEmpty()) {
            return OutfitSuggestionResponse.builder()
                    .weatherSummary(weatherSummary)
                    .reasoning("Your wardrobe is empty. Please add some clothing items first.")
                    .build();
        }

        // Step 2b: Get user preferences
        String preferencesLine = "";
        UserProfile profile = userProfileRepository.findByUserId(userId).orElse(null);
        if (profile != null) {
            StringBuilder pref = new StringBuilder();
            if (profile.getGenderAesthetic() != null) pref.append(profile.getGenderAesthetic()).append(" styling");
            if (profile.getModestyLevel() != null) {
                if (pref.length() > 0) pref.append(", ");
                pref.append(profile.getModestyLevel()).append(" modesty level");
            }
            if (profile.getCulturalPreferences() != null && !profile.getCulturalPreferences().isEmpty()) {
                if (pref.length() > 0) pref.append(", and has the following cultural preferences: ");
                pref.append(String.join(", ", profile.getCulturalPreferences()));
            }
            if (pref.length() > 0) {
                preferencesLine = "The user prefers " + pref + ". All outfit suggestions must respect these preferences.\n";
            }
        }

        // Step 2b2: Get colour profile
        String colourLine = "";
        if (profile != null && profile.getColourSeason() != null) {
            StringBuilder col = new StringBuilder();
            col.append("The user's colour season is ").append(profile.getColourSeason());
            if (profile.getColourUndertone() != null) {
                col.append(" with ").append(profile.getColourUndertone().toLowerCase()).append(" undertones");
            }
            if (profile.getRecommendedColors() != null && !profile.getRecommendedColors().isEmpty()) {
                col.append(". Their most flattering clothing colors are: ")
                   .append(String.join(", ", profile.getRecommendedColors()));
            }
            col.append(". Where possible, prefer items in these colors or colors that complement their natural coloring.\n");
            colourLine = col.toString();
        }

        // Step 2b3: Get body shape profile
        String bodyShapeLine = "";
        if (profile != null && profile.getBodyShape() != null) {
            StringBuilder body = new StringBuilder();
            body.append("The user's body shape is ").append(profile.getBodyShape()).append(".");
            if (profile.getBodyBestStyles() != null && !profile.getBodyBestStyles().isEmpty()) {
                body.append(" Styles that flatter them: ").append(String.join(", ", profile.getBodyBestStyles())).append(".");
            }
            if (profile.getBodyStylesToAvoid() != null && !profile.getBodyStylesToAvoid().isEmpty()) {
                body.append(" Styles to avoid: ").append(String.join(", ", profile.getBodyStylesToAvoid())).append(".");
            }
            body.append(" Where possible, prefer clothing cuts that suit their body shape.\n");
            bodyShapeLine = body.toString();
        }

        // Step 2c: Get taste profile (built from user's outfit ratings)
        String tasteProfileLine = "";
        UserTasteProfile tasteProfile = userTasteProfileRepository.findByUserId(userId).orElse(null);
        if (tasteProfile != null && tasteProfile.getTotalRatings() >= 3) {
            tasteProfileLine = String.format(
                "Learning from this user's outfit ratings:\n" +
                "They LOVE combinations like: %s\n" +
                "Their favorite colors: %s\n" +
                "Their favorite styles: %s\n" +
                "They tend to DISLIKE: %s\n" +
                "Colors they avoid: %s\n" +
                "Strongly weight these preferences when selecting from their wardrobe.\n",
                tasteProfile.getLovedCombinations() != null ? String.join(", ", tasteProfile.getLovedCombinations()) : "",
                tasteProfile.getFavoriteColors() != null ? String.join(", ", tasteProfile.getFavoriteColors()) : "",
                tasteProfile.getFavoriteStyles() != null ? String.join(", ", tasteProfile.getFavoriteStyles()) : "",
                tasteProfile.getDislikedCombinations() != null ? String.join(", ", tasteProfile.getDislikedCombinations()) : "",
                tasteProfile.getAvoidedColors() != null ? String.join(", ", tasteProfile.getAvoidedColors()) : ""
            );
        }

        // Step 3: Build Gemini outfit selection prompt
        String itemsList = wardrobe.stream()
                .map(item -> String.format(
                    "id: %s, type: %s, color: %s, style: %s, occasions: %s",
                    item.getId(),
                    item.getTags() != null ? item.getTags().getType() : "unknown",
                    item.getTags() != null ? item.getTags().getColor() : "unknown",
                    item.getTags() != null ? item.getTags().getStyle() : "unknown",
                    item.getTags() != null && item.getTags().getOccasion() != null
                        ? String.join(", ", item.getTags().getOccasion()) : "unknown"
                ))
                .collect(Collectors.joining("\n"));

        String selectionPrompt = String.format(
            "You are a fashion expert helping a user get dressed for a specific occasion.\n\n" +
            "Occasion: %s\n" +
            "Weather: %.1f°C, %s in %s\n\n" +
            "Their wardrobe:\n%s\n\n" +
            "%s" +
            "%s" +
            "%s" +
            "%s" +
            "STEP 1 — Evaluate suitability. Ask yourself: does this wardrobe contain items that are " +
            "GENUINELY appropriate for '%s'? Apply strict real-world standards:\n" +
            "- Gym / workout / sport: requires athletic shorts, joggers, leggings, sports top, " +
            "  athletic shoes. Jeans, dress pants, formal shirts, boots are NOT acceptable.\n" +
            "- Formal / business / interview: requires dress pants/skirt, blouse/shirt/blazer, " +
            "  dress shoes. Hoodies, sneakers, casual tees are NOT acceptable.\n" +
            "- Beach / swim: requires swimwear, flip flops, cover-ups. Jeans are NOT acceptable.\n" +
            "- Apply the same logic for any other occasion — be strict and honest.\n\n" +
            "STEP 2 — Based on your evaluation:\n" +
            "  A) If the wardrobe HAS appropriate items: select the best combination. " +
            "     Set selectedItemIds to those item ids and write a short reasoning explaining why " +
            "     the outfit works for the occasion and weather. Also mention if the chosen colors " +
            "     complement the user's colour season.\n" +
            "  B) If the wardrobe DOES NOT have appropriate items: set selectedItemIds to an empty " +
            "     array []. In reasoning, write: 'Your wardrobe does not have suitable [specific " +
            "     item type] for [occasion]. Consider buying: [list 2-3 specific item types with " +
            "     a one-line reason each].' Be specific — name the exact clothing types missing.\n\n" +
            "Return ONLY valid JSON, no markdown, no extra text:\n" +
            "{\"selectedItemIds\": [...], \"reasoning\": \"...\"}",
            occasion, weather.temp(), weather.description(), weather.city(),
            itemsList, preferencesLine, colourLine, bodyShapeLine, tasteProfileLine, occasion
        );

        // Step 4: Call Gemini for outfit selection
        String geminiResponse = geminiService.sendTextPrompt(selectionPrompt);
        if (geminiResponse == null) {
            return OutfitSuggestionResponse.builder()
                    .weatherSummary(weatherSummary)
                    .reasoning("Outfit suggestion service is temporarily unavailable. Please try again.")
                    .build();
        }

        // Step 5: Parse Gemini JSON response
        List<String> selectedItemIds;
        String reasoning;
        try {
            String cleaned = GeminiUtils.stripMarkdownCodeBlock(geminiResponse);
            Map<String, Object> parsed = objectMapper.readValue(cleaned, new TypeReference<>() {});
            Object ids = parsed.get("selectedItemIds");
            if (ids instanceof List<?> list) {
                selectedItemIds = list.stream().map(Object::toString).toList();
            } else {
                selectedItemIds = List.of();
            }
            reasoning = parsed.get("reasoning") != null ? parsed.get("reasoning").toString() : "";
        } catch (Exception e) {
            log.error("Failed to parse Gemini outfit selection response: {}", e.getMessage());
            return OutfitSuggestionResponse.builder()
                    .weatherSummary(weatherSummary)
                    .reasoning("Could not parse outfit suggestion. Please try again.")
                    .build();
        }

        // Step 6: Build response with real item images
        List<OutfitSuggestionResponse.SelectedItem> selectedItems = wardrobe.stream()
                .filter(item -> selectedItemIds.contains(item.getId()))
                .map(item -> OutfitSuggestionResponse.SelectedItem.builder()
                        .itemId(item.getId())
                        .type(item.getTags() != null ? item.getTags().getType() : "unknown")
                        .color(item.getTags() != null ? item.getTags().getColor() : "unknown")
                        .imageBase64(item.getGeneratedImageBase64())
                        .build())
                .toList();

        // Step 7: Return response
        return OutfitSuggestionResponse.builder()
                .selectedItems(selectedItems)
                .reasoning(reasoning)
                .weatherSummary(weatherSummary)
                .build();
    }
}
