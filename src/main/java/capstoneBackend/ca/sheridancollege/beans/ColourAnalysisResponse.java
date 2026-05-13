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
public class ColourAnalysisResponse {

    private String season;
    private String description;
    private ColourPalette palette;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ColourPalette {
        private List<String> tops;
        private List<String> bottoms;
        private List<String> outerwear;
        private List<String> shoes;
    }
}
