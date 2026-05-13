package capstoneBackend.ca.sheridancollege.beans;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PackingRequest {
    private String destination;
    private int tripLengthDays;
    private List<String> activities;
}
