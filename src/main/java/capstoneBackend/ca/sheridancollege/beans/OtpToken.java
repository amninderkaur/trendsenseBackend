package capstoneBackend.ca.sheridancollege.beans;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "otp_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpToken {
    @Id
    private String id;
    private String email;
    private String otp;
    private LocalDateTime expiresAt;
    private boolean used;
}
