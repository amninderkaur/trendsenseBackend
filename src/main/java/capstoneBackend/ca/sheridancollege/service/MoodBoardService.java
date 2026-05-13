package capstoneBackend.ca.sheridancollege.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import capstoneBackend.ca.sheridancollege.beans.MoodBoard;
import capstoneBackend.ca.sheridancollege.beans.MoodBoardMatchRequest;
import capstoneBackend.ca.sheridancollege.beans.MoodBoardRequest;
import capstoneBackend.ca.sheridancollege.beans.MoodBoardResponse;
import capstoneBackend.ca.sheridancollege.beans.UserProfile;
import capstoneBackend.ca.sheridancollege.beans.WardrobeItem;
import capstoneBackend.ca.sheridancollege.beans.repositories.MoodBoardRepository;
import capstoneBackend.ca.sheridancollege.beans.repositories.UserProfileRepository;
import capstoneBackend.ca.sheridancollege.beans.repositories.WardrobeRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@AllArgsConstructor
public class MoodBoardService {

    private final MoodBoardRepository moodBoardRepository;
    private final WardrobeRepository wardrobeRepository;
    private final UserProfileRepository userProfileRepository;
    private final GeminiService geminiService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MoodBoardResponse saveMoodBoard(String userId, MoodBoardRequest request) {
        MoodBoard board = new MoodBoard();
        board.setUserId(userId);
        board.setMood(request.getMood());
        board.setCreatedAt(Instant.now());

        if (request.getSavedOutfits() != null) {
            List<MoodBoard.SavedOutfit> outfits = request.getSavedOutfits().stream()
                    .map(o -> new MoodBoard.SavedOutfit(o.getItemIds(), o.getDescription()))
                    .collect(Collectors.toList());
            board.setSavedOutfits(outfits);
        }

        MoodBoard saved = moodBoardRepository.save(board);

        List<MoodBoardResponse.SavedOutfit> responseOutfits = saved.getSavedOutfits() != null
                ? saved.getSavedOutfits().stream()
                    .map(o -> new MoodBoardResponse.SavedOutfit(o.getItemIds(), o.getDescription()))
                    .collect(Collectors.toList())
                : null;

        return MoodBoardResponse.builder()
                .id(saved.getId())
                .mood(saved.getMood())
                .savedOutfits(responseOutfits)
                .createdAt(saved.getCreatedAt())
                .build();
    }

    public List<MoodBoardResponse> getMoodBoards(String userId) {
        return moodBoardRepository.findByUserId(userId).stream()
                .map(board -> {
                    List<MoodBoardResponse.SavedOutfit> outfits = board.getSavedOutfits() != null
                            ? board.getSavedOutfits().stream()
                                .map(o -> new MoodBoardResponse.SavedOutfit(o.getItemIds(), o.getDescription()))
                                .collect(Collectors.toList())
                            : null;
                    return MoodBoardResponse.builder()
                            .id(board.getId())
                            .mood(board.getMood())
                            .savedOutfits(outfits)
                            .createdAt(board.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    public MoodBoardResponse matchOutfits(String userId, MoodBoardMatchRequest request) {

        // Step 1: Fetch wardrobe
        List<WardrobeItem> wardrobe = wardrobeRepository.findByUserId(userId);
        if (wardrobe.isEmpty()) {
            return MoodBoardResponse.builder()
                    .mood(request.getMood())
                    .outfitSuggestions(List.of())
                    .build();
        }

        // Step 2: Build wardrobe description for prompt (include itemId so Gemini can reference it)
        String wardrobeList = wardrobe.stream()
                .map(item -> String.format("id: %s, name: %s, category: %s",
                        item.getId(),
                        item.getTag() != null ? item.getTag() : "unknown",
                        item.getTag() != null ? item.getTag() : "unknown"))
                .collect(Collectors.joining("\n"));

        // Step 3: Fetch user preferences
        String preferencesText = buildPreferencesText(userId);

        // Step 4: Build Gemini prompt
        String prompt = String.format(
            "You are a personal stylist. The user's wardrobe contains these items:\n%s\n\n" +
            "Mood: %s\n" +
            "Occasion: %s\n" +
            "Weather: %s\n" +
            "%s\n\n" +
            "Suggest 2-3 outfit combinations using ONLY items from the wardrobe list above. " +
            "Use the exact itemId values provided.\n\n" +
            "Return a JSON object:\n" +
            "{\n" +
            "  \"outfitSuggestions\": [\n" +
            "    {\n" +
            "      \"description\": \"brief outfit description\",\n" +
            "      \"items\": [\n" +
            "        {\"itemId\": \"<id from wardrobe>\", \"name\": \"<item name>\", \"category\": \"<category>\"}\n" +
            "      ]\n" +
            "    }\n" +
            "  ]\n" +
            "}\n" +
            "Respond in valid JSON only. No extra text, no markdown, no code blocks.",
            wardrobeList,
            request.getMood(),
            request.getOccasion(),
            request.getWeather(),
            preferencesText.isEmpty() ? "" : "User preferences: " + preferencesText
        );

        // Step 5: Call Gemini
        String geminiResponse = geminiService.sendTextPrompt(prompt);
        if (geminiResponse == null) {
            log.error("Gemini returned null for moodboard match");
            return MoodBoardResponse.builder()
                    .mood(request.getMood())
                    .outfitSuggestions(List.of())
                    .build();
        }

        // Step 6: Parse response
        try {
            String cleaned = stripMarkdown(geminiResponse);
            Map<String, Object> parsed = objectMapper.readValue(cleaned, new TypeReference<>() {});

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> suggestions = (List<Map<String, Object>>) parsed.get("outfitSuggestions");

            List<MoodBoardResponse.OutfitSuggestion> outfitSuggestions = suggestions == null
                    ? List.of()
                    : suggestions.stream().map(s -> {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> items = (List<Map<String, Object>>) s.get("items");
                        List<MoodBoardResponse.SuggestedItem> suggestedItems = items == null
                                ? List.of()
                                : items.stream().map(i -> MoodBoardResponse.SuggestedItem.builder()
                                        .itemId(str(i, "itemId"))
                                        .name(str(i, "name"))
                                        .category(str(i, "category"))
                                        .build())
                                    .collect(Collectors.toList());
                        return MoodBoardResponse.OutfitSuggestion.builder()
                                .description(str(s, "description"))
                                .items(suggestedItems)
                                .build();
                    }).collect(Collectors.toList());

            return MoodBoardResponse.builder()
                    .mood(request.getMood())
                    .outfitSuggestions(outfitSuggestions)
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse moodboard match response: {}", e.getMessage());
            return MoodBoardResponse.builder()
                    .mood(request.getMood())
                    .outfitSuggestions(List.of())
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
            sb.append(profile.getModestyLevel()).append(" modesty level");
        }
        if (profile.getCulturalPreferences() != null && !profile.getCulturalPreferences().isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(String.join(", ", profile.getCulturalPreferences()));
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

    private String str(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : "";
    }
}
