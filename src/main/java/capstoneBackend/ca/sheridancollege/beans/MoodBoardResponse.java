package capstoneBackend.ca.sheridancollege.beans;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MoodBoardResponse {

    // Save / fetch fields
    private String id;
    private Instant createdAt;

    // Shared
    private String mood;
    private List<SavedOutfit> savedOutfits;

    // Match fields
    private List<OutfitSuggestion> outfitSuggestions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SavedOutfit {
        private List<String> itemIds;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OutfitSuggestion {
        private String description;
        private List<SuggestedItem> items;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SuggestedItem {
        private String itemId;
        private String name;
        private String category;
    }
}
