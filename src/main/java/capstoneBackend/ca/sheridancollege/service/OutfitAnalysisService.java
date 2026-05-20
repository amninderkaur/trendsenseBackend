package capstoneBackend.ca.sheridancollege.service;

import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import capstoneBackend.ca.sheridancollege.beans.OutfitAnalysisResponse;
import capstoneBackend.ca.sheridancollege.beans.UserProfile;
import capstoneBackend.ca.sheridancollege.beans.repositories.UserProfileRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@AllArgsConstructor
public class OutfitAnalysisService {

    private final WeatherService weatherService;
    private final GeminiService geminiService;
    private final UserProfileRepository userProfileRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public OutfitAnalysisResponse analyzeOutfit(String userId, MultipartFile image, String city, String occasion) {

        // Step 1: Fetch weather (optional — skipped if city not provided)
        WeatherService.WeatherInfo weather = null;
        String weatherDescription = "not available";
        if (city != null && !city.isBlank()) {
            weather = weatherService.getWeather(city);
            if (weather == null) {
                return OutfitAnalysisResponse.builder()
                        .overallVerdict("Could not fetch weather for city: " + city + ". Please check the city name and try again.")
                        .build();
            }
            weatherDescription = String.format("%.1f°C, %s in %s", weather.temp(), weather.description(), weather.city());
            log.info("Weather for {}: {}", city, weatherDescription);
        }

        // Step 2: Fetch user profile
        UserProfile profile = userProfileRepository.findByUserId(userId).orElse(null);
        String genderAesthetic = profile != null && profile.getGenderAesthetic() != null ? profile.getGenderAesthetic() : "unspecified";
        String modestyLevel = profile != null && profile.getModestyLevel() != null ? profile.getModestyLevel() : "unspecified";
        String culturalPreferences = (profile != null && profile.getCulturalPreferences() != null && !profile.getCulturalPreferences().isEmpty())
                ? String.join(", ", profile.getCulturalPreferences())
                : "none";
        String season = profile != null && profile.getColourSeason() != null ? profile.getColourSeason() : "unspecified";

        // Step 3: Convert image to Base64 in memory only
        String base64Image;
        String mimeType;
        try {
            base64Image = Base64.getEncoder().encodeToString(image.getBytes());
            mimeType = image.getContentType();
        } catch (Exception e) {
            log.error("Failed to read uploaded image: {}", e.getMessage());
            return OutfitAnalysisResponse.builder()
                    .overallVerdict("Could not read the uploaded image. Please try again.")
                    .build();
        }

        // Step 4: Build prompt and call Gemini with the image
        String occasionContext = "The user is planning to wear this for: " + occasion + ". ";
        String weatherContext = (weather != null)
                ? String.format("The person is in %s. Current weather: %s, %.1f°C. ", weather.city(), weather.description(), weather.temp())
                : "No location provided — skip weather evaluation. ";

        String prompt = String.format(
                "Analyze this outfit. %s" +
                "%s" +
                "User preferences: %s aesthetic, %s modesty, cultural preferences: %s. " +
                "Their colour season is %s. " +
                "Evaluate: 1) What occasion is this outfit suitable for? " +
                "2) Rate overall style out of 10. " +
                "3) Is it appropriate for the current weather (if weather is available)? " +
                "4) What works well? " +
                "5) What specific changes would improve it — different colours, add or remove layers, swap specific items? " +
                "Return ONLY valid JSON with fields: occasion, styleScore, " +
                "weatherVerdict (perfect/acceptable/not suitable/unknown), weatherReason, " +
                "whatWorksWell (array), suggestions (array), overallVerdict, currentWeather. " +
                "No markdown, no extra text.",
                weatherContext,
                occasionContext,
                genderAesthetic, modestyLevel, culturalPreferences, season
        );

        String geminiResponse = geminiService.sendImagePrompt(base64Image, mimeType, prompt);
        if (geminiResponse == null) {
            return OutfitAnalysisResponse.builder()
                    .currentWeather(weather != null ? weatherDescription : null)
                    .overallVerdict("Outfit analysis service is temporarily unavailable. Please try again.")
                    .build();
        }

        // Step 5: Strip markdown backticks, parse JSON, map to OutfitAnalysisResponse
        try {
            String cleaned = geminiResponse.trim();
            if (cleaned.startsWith("```")) {
                int firstNewline = cleaned.indexOf('\n');
                if (firstNewline != -1) cleaned = cleaned.substring(firstNewline + 1);
                if (cleaned.endsWith("```")) cleaned = cleaned.substring(0, cleaned.lastIndexOf("```")).trim();
            }

            Map<String, Object> parsed = objectMapper.readValue(cleaned, new TypeReference<>() {});

            List<String> whatWorksWell = toStringList(parsed.get("whatWorksWell"));
            List<String> suggestions = toStringList(parsed.get("suggestions"));

            return OutfitAnalysisResponse.builder()
                    .occasion(getString(parsed, "occasion"))
                    .styleScore(getInt(parsed, "styleScore"))
                    .weatherVerdict(getString(parsed, "weatherVerdict"))
                    .weatherReason(getString(parsed, "weatherReason"))
                    .whatWorksWell(whatWorksWell)
                    .suggestions(suggestions)
                    .overallVerdict(getString(parsed, "overallVerdict"))
                    .currentWeather(getString(parsed, "currentWeather"))
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse Gemini outfit analysis response: {}", e.getMessage());
            return OutfitAnalysisResponse.builder()
                    .currentWeather(weatherDescription)
                    .overallVerdict("Could not parse outfit analysis. Please try again.")
                    .build();
        }
    }

    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : "";
    }

    private int getInt(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Number n) return n.intValue();
        if (val != null) {
            try { return Integer.parseInt(val.toString()); } catch (NumberFormatException ignored) {}
        }
        return 0;
    }

    @SuppressWarnings("unchecked")
    private List<String> toStringList(Object val) {
        if (val instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        return List.of();
    }
}
