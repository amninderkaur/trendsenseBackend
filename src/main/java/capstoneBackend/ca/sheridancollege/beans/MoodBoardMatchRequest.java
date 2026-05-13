package capstoneBackend.ca.sheridancollege.beans;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MoodBoardMatchRequest {
    private String mood;
    private String occasion;
    private String weather;
}
