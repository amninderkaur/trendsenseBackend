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
public class BodyAnalysisResponse {

    // Set when photo validation fails (not a full body photo)
    private String error;
    private String errorReason;

    private String bodyShape;           // e.g. "Hourglass", "Pear", "Apple", "Rectangle", "Inverted Triangle"
    private String summary;             // 2-3 sentence description
    private List<String> bestStyles;    // silhouettes/cuts that flatter this shape
    private List<String> stylesToAvoid; // styles that don't suit this shape
    private List<String> tips;          // specific styling tips
}
