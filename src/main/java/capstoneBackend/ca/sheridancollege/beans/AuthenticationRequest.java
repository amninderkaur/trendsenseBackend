package capstoneBackend.ca.sheridancollege.beans;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationRequest {
    private String email;
    private String password;
    private String name;
    private String phoneNumber;
    private String deliveryMethod; // "email" or "sms"
}
