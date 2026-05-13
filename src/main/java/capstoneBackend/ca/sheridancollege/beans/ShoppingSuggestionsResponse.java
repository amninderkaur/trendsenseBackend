package capstoneBackend.ca.sheridancollege.beans;

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
public class ShoppingSuggestionsResponse {

    private String season;
    private List<String> gapsIdentified;
    private String totalEstimate;
    private boolean withinBudget;
    private List<ShoppingSuggestion> suggestions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ShoppingSuggestion {
        private String item;
        private String category;
        private String whyItFits;
        private String estimatedPrice;
        private String storeName;
        private String storeType;
        private String link;
        private String nearbyLocation;
    }
}
