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
public class OutfitAnalysisResponse {

    /** What occasion the outfit suits, e.g. "casual brunch", "business meeting" */
    private String occasion;

    /** Overall style score out of 10 */
    private int styleScore;

    /** One of: "perfect" | "acceptable" | "not suitable" */
    private String weatherVerdict;

    /** Explanation of weather suitability, e.g. "It's 4°C and raining — this outfit will leave you cold" */
    private String weatherReason;

    /** Positives about the outfit */
    private List<String> whatWorksWell;

    /** Specific changes that would improve it */
    private List<String> suggestions;

    /** One punchy sentence summarising the overall verdict */
    private String overallVerdict;

    /** Current weather at the requested city, e.g. "Cloudy, 8°C, light rain" */
    private String currentWeather;
}
