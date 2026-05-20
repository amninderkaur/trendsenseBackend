package capstoneBackend.ca.sheridancollege.beans;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "saved_shopping_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavedShoppingItem {

    @Id
    private String id;

    private String userId;
    private String item;
    private String category;
    private String whyItFits;
    private String estimatedPrice;
    private String storeName;
    private String storeType;
    private String link;
    private String nearbyLocation;
    private LocalDateTime savedAt;
}
