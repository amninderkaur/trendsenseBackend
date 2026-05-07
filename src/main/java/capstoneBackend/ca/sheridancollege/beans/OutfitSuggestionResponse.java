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
    private List<String> selectedItemIds;
    private String reasoning;
    private String weatherSummary;
    private String outfitImageBase64;
}
