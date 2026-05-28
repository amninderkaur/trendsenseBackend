package capstoneBackend.ca.sheridancollege.beans;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "outfit_ratings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutfitRating {

    @Id
    private String id;

    private String userId;
    private String outfitHistoryId;
    private int rating;
    private String occasion;
    private String weatherSummary;
    private List<String> itemTypes;
    private List<String> colors;
    private List<String> styles;
    private LocalDateTime ratedAt;
}
