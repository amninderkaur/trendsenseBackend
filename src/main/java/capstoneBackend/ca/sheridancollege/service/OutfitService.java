package capstoneBackend.ca.sheridancollege.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import capstoneBackend.ca.sheridancollege.beans.ClothingItem;
import capstoneBackend.ca.sheridancollege.beans.OutfitSuggestionResponse;
import capstoneBackend.ca.sheridancollege.beans.repositories.ClothingRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@AllArgsConstructor
public class OutfitService {

    private final ClothingRepository clothingRepository;
    private final GeminiService geminiService;
    private final WeatherService weatherService;

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
            "The user has these wardrobe items:\n%s\n\n" +
            "Current weather in %s is %.1f°C and %s.\n" +
            "The occasion is %s.\n" +
            "Select the best outfit from ONLY these items.\n" +
            "Return ONLY a valid JSON object with these fields:\n" +
            "selectedItemIds (array of item id strings),\n" +
            "reasoning (one sentence explaining why this outfit works for the weather and occasion).\n" +
            "Return ONLY the JSON, no markdown, no extra text.",
            itemsList, weather.city(), weather.temp(), weather.description(), occasion
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
            String cleaned = geminiResponse.trim();
            // Strip markdown code block if present
            if (cleaned.startsWith("```")) {
                int firstNewline = cleaned.indexOf('\n');
                if (firstNewline != -1) cleaned = cleaned.substring(firstNewline + 1);
                if (cleaned.endsWith("```")) cleaned = cleaned.substring(0, cleaned.lastIndexOf("```")).trim();
            }
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

        // Step 6: Build image generation prompt from selected items
        List<ClothingItem> selectedItems = wardrobe.stream()
                .filter(item -> selectedItemIds.contains(item.getId()))
                .toList();

        String itemDescriptions = selectedItems.stream()
                .map(item -> item.getTags() != null
                    ? item.getTags().getColor() + " " + item.getTags().getType()
                    : "clothing item")
                .collect(Collectors.joining(", "));

        String imagePrompt = String.format(
            "A professional fashion photography shot of a complete outfit: %s. " +
            "White background, studio lighting, clothing laid flat or on invisible mannequin, " +
            "clean product style photo.",
            itemDescriptions
        );

        // Step 7: Generate outfit image
        log.info("Generating outfit image for: {}", imagePrompt);
        String outfitImageBase64 = geminiService.generateItemImage(imagePrompt);

        // Step 8: Return response
        return OutfitSuggestionResponse.builder()
                .selectedItemIds(selectedItemIds)
                .reasoning(reasoning)
                .weatherSummary(weatherSummary)
                .outfitImageBase64(outfitImageBase64)
                .build();
    }
}
