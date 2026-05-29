package capstoneBackend.ca.sheridancollege.beans;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for the style trends endpoint.
 * Not persisted in MongoDB — always fetched fresh from Gemini.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StyleTrendResult {

    private String season;
    private String year;
    private List<TrendItem> trends;
    private List<WardrobeMatch> wardrobeMatches;
    private String summary;
    private LocalDateTime fetchedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendItem {
        /** e.g. "Quiet Luxury" */
        private String trendName;
        private String description;
        /** e.g. ["cashmere sweater", "tailored trousers"] */
        private List<String> keyPieces;
        /** Trending colors for this trend */
        private List<String> colors;
        /** One sentence styling tip */
        private String wearItHow;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WardrobeMatch {
        private String itemId;
        private String itemType;
        private String color;
        private String imageBase64;
        /** Which trends this wardrobe item fits */
        private List<String> matchingTrends;
        /** How to wear this item on-trend right now */
        private String stylingTip;
    }
}
