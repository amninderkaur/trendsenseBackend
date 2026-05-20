package capstoneBackend.ca.sheridancollege.beans;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "reviews")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Review {

    @Id
    private String id;
    private long caseNumber;
    private String userId;
    private String email;
    private String message;
    private int rating; // 1-5
    private String adminReply;
    private Instant createdAt;
}
