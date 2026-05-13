package capstoneBackend.ca.sheridancollege.beans;

import java.time.Instant;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "moodboards")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MoodBoard {

    @Id
    private String id;
    private String userId;
    private String mood;
    private List<SavedOutfit> savedOutfits;
    private Instant createdAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SavedOutfit {
        private List<String> itemIds;
        private String description;
    }
}
