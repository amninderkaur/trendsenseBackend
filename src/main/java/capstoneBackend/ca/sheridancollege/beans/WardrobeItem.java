package capstoneBackend.ca.sheridancollege.beans;

import java.util.Date;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "wardrobe")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WardrobeItem {
	@Id
    private String id;
    private String userId;
    private String imageUrl;
    private List<String> detectedItems;
    private Date uploadDate;
    private String tag;
    private List<String> cropUrls;
    private int wearCount = 0;
}
