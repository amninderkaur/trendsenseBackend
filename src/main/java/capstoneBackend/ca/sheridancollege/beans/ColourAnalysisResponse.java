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

    // Set when photo validation fails (no face detected)
    private String error;
    private String errorReason;

    private String season;
    private String undertone;
    private String contrast;
    private String bestJewelry;
    private String summary;
    private List<String> recommendedColors;
}
