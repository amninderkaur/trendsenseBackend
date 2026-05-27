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
    private String undertone;
    private String contrast;
    private String bestJewelry;
    private String summary;
    private List<String> recommendedColors;
}
