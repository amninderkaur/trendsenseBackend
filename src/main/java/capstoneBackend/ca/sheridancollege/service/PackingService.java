package capstoneBackend.ca.sheridancollege.service;

import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import capstoneBackend.ca.sheridancollege.beans.PackingListResponse;
import capstoneBackend.ca.sheridancollege.beans.PackingRequest;
import capstoneBackend.ca.sheridancollege.beans.UserProfile;
import capstoneBackend.ca.sheridancollege.beans.repositories.UserProfileRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@AllArgsConstructor
public class PackingService {

    private final GeminiService geminiService;
    private final WeatherService weatherService;
    private final UserProfileRepository userProfileRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PackingListResponse suggestPacking(String userId, PackingRequest request) {

        // Step 1: Fetch weather for destination
        WeatherService.WeatherInfo weather = weatherService.getWeather(request.getDestination());
        String weatherSummary = weather != null
                ? String.format("%.1f°C, %s in %s", weather.temp(), weather.description(), weather.city())
                : "Weather data unavailable for " + request.getDestination();

        // Step 2: Fetch user preferences
        String preferencesText = buildPreferencesText(userId);

        // Step 3: Build Gemini prompt
        String activitiesText = request.getActivities() != null
                ? String.join(", ", request.getActivities())
                : "general sightseeing";

        String prompt = String.format(
            "You are a travel packing expert and personal stylist.\n" +
            "The user is planning a trip:\n" +
            "- Destination: %s\n" +
            "- Trip length: %d days\n" +
            "- Activities: %s\n" +
            "- Weather: %s\n" +
            "%s\n\n" +
            "Generate a practical packing list with specific items and quantities (e.g. 'Light knit sweater x3').\n\n" +
            "Return a JSON object with exactly these fields:\n" +
            "{\n" +
            "  \"weatherSummary\": \"<brief weather description>\",\n" +
            "  \"packingList\": {\n" +
            "    \"tops\": [\"item x qty\", ...],\n" +
            "    \"bottoms\": [\"item x qty\", ...],\n" +
            "    \"outerwear\": [\"item x qty\", ...],\n" +
            "    \"shoes\": [\"item x qty\", ...],\n" +
            "    \"accessories\": [\"item x qty\", ...],\n" +
            "    \"extras\": [\"item\", ...]\n" +
            "  },\n" +
            "  \"tips\": \"<one sentence packing tip>\"\n" +
            "}\n" +
            "Respond in valid JSON only. No extra text, no markdown, no code blocks.",
            request.getDestination(),
            request.getTripLengthDays(),
            activitiesText,
            weatherSummary,
            preferencesText.isEmpty() ? "" : "- User style preferences: " + preferencesText
        );

        // Step 4: Call Gemini
        String geminiResponse = geminiService.sendTextPrompt(prompt);
        if (geminiResponse == null) {
            log.error("Gemini returned null for packing suggestion");
            return PackingListResponse.builder()
                    .destination(request.getDestination())
                    .weatherSummary(weatherSummary)
                    .tips("Packing suggestion service is temporarily unavailable. Please try again.")
                    .build();
        }

        // Step 5: Parse response
        try {
            String cleaned = stripMarkdown(geminiResponse);
            PackingListResponse parsed = objectMapper.readValue(cleaned, PackingListResponse.class);
            return PackingListResponse.builder()
                    .destination(request.getDestination())
                    .weatherSummary(parsed.getWeatherSummary() != null ? parsed.getWeatherSummary() : weatherSummary)
                    .packingList(parsed.getPackingList())
                    .tips(parsed.getTips())
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse packing suggestion response: {}", e.getMessage());
            return PackingListResponse.builder()
                    .destination(request.getDestination())
                    .weatherSummary(weatherSummary)
                    .tips("Could not parse packing suggestion. Please try again.")
                    .build();
        }
    }

    private String buildPreferencesText(String userId) {
        UserProfile profile = userProfileRepository.findByUserId(userId).orElse(null);
        if (profile == null) return "";

        StringBuilder sb = new StringBuilder();
        if (profile.getGenderAesthetic() != null) sb.append(profile.getGenderAesthetic()).append(" aesthetic");
        if (profile.getModestyLevel() != null) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(profile.getModestyLevel()).append(" modesty");
        }
        if (profile.getCulturalPreferences() != null && !profile.getCulturalPreferences().isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(String.join(", ", profile.getCulturalPreferences()));
        }
        if (profile.getColourSeason() != null) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(profile.getColourSeason()).append(" colour season");
        }
        return sb.toString();
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
