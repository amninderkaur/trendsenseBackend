package capstoneBackend.ca.sheridancollege.beans;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PackingListResponse {

    private String destination;
    private String weatherSummary;
    private PackingCategories packingList;
    private String tips;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PackingCategories {
        private List<String> tops;
        private List<String> bottoms;
        private List<String> outerwear;
        private List<String> shoes;
        private List<String> accessories;
        private List<String> extras;
    }
}
