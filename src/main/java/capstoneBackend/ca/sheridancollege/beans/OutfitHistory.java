package capstoneBackend.ca.sheridancollege.beans;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "outfit_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutfitHistory {

    @Id
    private String id;

    private String userId;
    private String occasion;
    private String city;
    private String weatherSummary;
    private String reasoning;
    private List<OutfitSuggestionResponse.SelectedItem> selectedItems;
    private LocalDateTime savedAt;
}
