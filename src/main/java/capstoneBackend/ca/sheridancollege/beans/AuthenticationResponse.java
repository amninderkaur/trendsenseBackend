package capstoneBackend.ca.sheridancollege.beans;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationResponse {
    private String token;
    private String userId;
    private String role;
    private String name;
    private String profilePicture;      // Base64-encoded image for the frontend
    private String profilePictureType;
    private String message;
    private Boolean requiresOtp;
    private String deliveryMethod;
}