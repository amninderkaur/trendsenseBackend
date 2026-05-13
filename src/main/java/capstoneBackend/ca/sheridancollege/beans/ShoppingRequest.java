package capstoneBackend.ca.sheridancollege.beans;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShoppingRequest {
    private String destination;
    private double budget;
    private String currency;
    private String location;
    private String focusCategory;
    private boolean preferOnline;
}
