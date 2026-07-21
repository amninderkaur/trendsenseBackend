package capstoneBackend.ca.sheridancollege.beans;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Document(collection = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails{
	@Id
    private String id;
    private String email;
    private String password;
    private Role role;
    private String name;
    private byte[] profilePicture;         // image bytes stored in MongoDB
    private String profilePictureType;     // e.g. "image/jpeg"
    private String phoneNumber;
    private String deliveryMethod;         // "email" or "sms"
    @Field("hasLoggedInBefore")
    private boolean loggedInBefore;
    @Builder.Default
    private List<LocalDate> loginDates = new ArrayList<>();

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Role effectiveRole = role != null ? role : Role.USER;
        return List.of(new SimpleGrantedAuthority("ROLE_" + effectiveRole.name()));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

}
