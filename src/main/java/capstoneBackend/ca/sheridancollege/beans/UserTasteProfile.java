package capstoneBackend.ca.sheridancollege.beans;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "user_taste_profiles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserTasteProfile {

    @Id
    private String id;

    private String userId;
    private List<String> lovedCombinations;
    private List<String> dislikedCombinations;
    private List<String> favoriteColors;
    private List<String> avoidedColors;
    private List<String> favoriteStyles;
    private List<String> favoriteOccasions;
    private int totalRatings;
    private double averageRating;
    private LocalDateTime lastUpdated;
}
