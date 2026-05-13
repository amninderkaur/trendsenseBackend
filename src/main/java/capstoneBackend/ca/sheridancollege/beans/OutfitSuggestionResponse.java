package capstoneBackend.ca.sheridancollege.beans;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutfitSuggestionResponse {
    private List<SelectedItem> selectedItems;
    private String reasoning;
    private String weatherSummary;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SelectedItem {
        private String itemId;
        private String type;
        private String color;
        private String imageBase64;
    }
}
