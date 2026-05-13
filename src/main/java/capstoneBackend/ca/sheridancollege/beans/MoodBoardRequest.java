package capstoneBackend.ca.sheridancollege.beans;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MoodBoardRequest {

    private String mood;
    private List<SavedOutfit> savedOutfits;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SavedOutfit {
        private List<String> itemIds;
        private String description;
    }
}
